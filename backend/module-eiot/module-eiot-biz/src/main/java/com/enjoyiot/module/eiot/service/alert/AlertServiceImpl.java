
/*
 *
 *  * | Licensed 未经许可不能去掉「Enjoy-iot」相关版权
 *  * +----------------------------------------------------------------------
 *  * | Author: xw2sy@163.com | Tel: 19918996474
 *  * +----------------------------------------------------------------------
 *
 *  Copyright [2025] [Enjoy-iot] | Tel: 19918996474
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */
package com.enjoyiot.module.eiot.service.alert;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.api.alert.dto.AlertRecord;
import com.enjoyiot.module.eiot.controller.admin.alertconfig.vo.AlertRecordPageReq;
import com.enjoyiot.module.eiot.dal.dataobject.AlertRecordDO;
import com.enjoyiot.module.eiot.dal.dataobject.deviceinfo.EiotDeviceInfoDO;
import com.enjoyiot.module.eiot.dal.mysql.AlertRecordMapper;
import com.enjoyiot.module.eiot.dal.mysql.deviceinfo.EiotDeviceInfoMapper;
import com.enjoyiot.module.eiot.service.UserTenantResolver;
import com.enjoyiot.module.system.api.tenant.TenantApi;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.util.List;

@Service
public class AlertServiceImpl implements AlertService {
    @Resource
    private AlertRecordMapper alertRecordMapper;

    @Resource
    private EiotDeviceInfoMapper eiotDeviceInfoMapper;

    @Resource
    private TenantApi tenantApi;

    @Resource
    private UserTenantResolver userTenantResolver;

    @Override
    public PageResult<AlertRecord> selectAlertRecordPage(AlertRecordPageReq request) {
        // 按当前登录用户账号所属租户过滤；系统租户(admin)账号保留看全部（由租户拦截器规则保证）
        Long userTenantId = userTenantResolver.resolveLoginUserTenantId();
        PageResult<AlertRecord> result = userTenantId != null
                ? TenantUtils.execute(userTenantId, () -> alertRecordMapper.selectPage(request))
                : alertRecordMapper.selectPage(request);
        fillTenantName(result.getList());
        return result;
    }

    /**
     * 填充租户名称
     */
    private void fillTenantName(List<AlertRecord> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        list.forEach(r -> {
            if (r.getTenantId() == null) {
                return;
            }
            r.setTenantName(tenantApi.getTenantName(r.getTenantId()));
        });
    }

    @Override
    public void confirmAlertRecord(Long id) {
        AlertRecordDO updateDO = new AlertRecordDO();
        updateDO.setId(id);
        updateDO.setReadFlg(true);
        updateDO.setHandleStatus(3);
        alertRecordMapper.updateById(updateDO);
    }

    @Override
    public void deferAlertRecord(Long id) {
        AlertRecordDO updateDO = new AlertRecordDO();
        updateDO.setId(id);
        updateDO.setHandleStatus(1);
        alertRecordMapper.updateById(updateDO);
    }

    @Override
    public void handleAlertRecord(Long id) {
        AlertRecordDO updateDO = new AlertRecordDO();
        updateDO.setId(id);
        updateDO.setHandleStatus(2);
        updateDO.setReadFlg(true);
        alertRecordMapper.updateById(updateDO);
    }

    @Override
    public List<AlertRecord> getUnprocessedAlertsByParentDeviceId(Long parentDeviceId, int minutes) {
        if (parentDeviceId == null) {
            return List.of();
        }
        long threshold = System.currentTimeMillis() - (long) minutes * 60 * 1000;
        Long userTenantId = userTenantResolver.resolveLoginUserTenantId();
        return userTenantId != null
                ? TenantUtils.execute(userTenantId, () -> alertRecordMapper.selectUnprocessedAlertsByParentDeviceId(parentDeviceId, threshold))
                : alertRecordMapper.selectUnprocessedAlertsByParentDeviceId(parentDeviceId, threshold);
    }

    @Override
    public Long findTransferDeviceId(Long alertDeviceId) {
        if (alertDeviceId == null) return null;
        // 查找设备信息（忽略租户隔离）
        EiotDeviceInfoDO device = TenantUtils.executeIgnoreWithResult(
                () -> eiotDeviceInfoMapper.selectById(alertDeviceId));
        if (device == null) return null;
        // 如果本身是用传设备，直接返回
        if ("transfer".equals(device.getDeviceType())) {
            return device.getId();
        }
        // 递归查找父设备
        Long parentId = device.getParentId();
        int maxDepth = 5; // 防止无限递归
        int depth = 0;
        while (parentId != null && depth < maxDepth) {
            Long finalParentId = parentId;
            EiotDeviceInfoDO parent = TenantUtils.executeIgnoreWithResult(
                    () -> eiotDeviceInfoMapper.selectById(finalParentId));
            if (parent == null) break;
            if ("transfer".equals(parent.getDeviceType())) {
                return parent.getId();
            }
            parentId = parent.getParentId();
            depth++;
        }
        // 找不到用传设备，返回 null
        return null;
    }

    @Override
    public List<AlertRecord> getRecentUnprocessedAlerts(int minutes) {
        long threshold = System.currentTimeMillis() - (long) minutes * 60 * 1000;
        Long userTenantId = userTenantResolver.resolveLoginUserTenantId();
        return userTenantId != null
                ? TenantUtils.execute(userTenantId, () -> alertRecordMapper.selectRecentUnprocessedAlerts(threshold))
                : alertRecordMapper.selectRecentUnprocessedAlerts(threshold);
    }
}
