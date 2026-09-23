
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
package com.enjoyiot.module.eiot.service.device;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.enjoyiot.module.eiot.api.device.dto.DevicePropertyCache;
import com.enjoyiot.module.eiot.api.device.dto.DeviceShortInfo;
import com.enjoyiot.module.eiot.api.device.dto.RegisterDevice;
import com.enjoyiot.module.eiot.controller.admin.device.vo.*;
import com.enjoyiot.module.eiot.controller.admin.device.vo.devicegroup.DeviceImportRespVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;


/**
 * 设备信息 Service 接口
 *
 * @author EnjoyIot
 */
public interface DeviceInfoService {

    /**
     * 创建设备信息
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDeviceInfo(@Valid DeviceInfoSaveReqVO createReqVO);

    /**
     * 更新设备信息
     *
     * @param updateReqVO 更新信息
     */
    void updateDeviceInfo(@Valid DeviceInfoSaveReqVO updateReqVO);

    /**
     * 删除设备信息
     *
     * @param id 编号
     */
    Boolean deleteDeviceInfo(Long id);

    /**
     * 获得设备信息
     *
     * @param id 编号
     * @return 设备信息
     */
    DeviceInfo getDeviceInfo(Long id);

    DeviceInfo getDeviceByPkDnByCache(String pk, String dn);

    /**
     * 仅按设备名称(dn)查询设备，忽略租户隔离
     * 用于协议组件在设备接入时识别已预注册的设备
     *
     * @param dn 设备名称
     * @return 设备信息
     */
    DeviceInfo getDeviceByDn(String dn);

    /**
     * 根据父设备ID和设备名称(dn)查询子设备，忽略租户隔离
     * 用于协议组件在设备接入时按父设备精准识别子设备
     *
     * @param parentId 父设备ID
     * @param dn 设备名称
     * @return 子设备信息
     */
    DeviceInfo getDeviceByParentIdAndDn(Long parentId, String dn);

    DeviceInfo getDeviceInfoFromCache(Long deviceId);

    Map<String, DevicePropertyCache> getPropertiesFromCache(Long deviceId);

    /**
     * 获得设备信息分页
     *
     * @param pageReqVO 分页查询
     * @return 设备信息分页
     */
    PageResult<DeviceShortInfo> getDeviceInfoPage(DeviceInfoPageReqVO pageReqVO);

    DeviceInfo getDeviceBySerialNo(String serialNumber);

    boolean deleteByIds(List<Long> ids);

    DeviceImportRespVO importDevice(List<DeviceInfoImportVo> list, Long productId, Long parentId);

    List<DeviceInfo> findSubDeviceList(String productKey, String deviceName);

    PageResult<DeviceShortInfo> getUnbindPage(DeviceUnbindPageReqVO pageReqVO);

    void bindParent(DeviceBindReqVO saveReqVO);

    void unbindParent(DeviceUnbindReqVO unbindReqVO);

    DeviceInfo registerDevice(RegisterDevice registerDevice);

    long getLastTimeCache(Long deviceId);

    void updateDeviceLastTimeCache(Long deviceId, long lastTime);

    Boolean updateDeviceState(Long deviceId, boolean online);

    void savePropertiesCache(Long deviceId, Map<String, DevicePropertyCache> properties);

    void clearPropertiesCache(String productKey);

    List<DeviceInfo> getDeviceInfoList(List<Long> subDeviceIds);

    Boolean subDeRegisterDevice(String pk, String dn, String subPkDeregister, String subDnDeregister1);

    /**
     * 批量分配租户（仅允许对未分配租户的设备进行操作）
     *
     * @param deviceIds 设备ID列表
     * @param tenantId 租户ID
     * @return 分配结果（包含成功数、跳过数、总数）
     */
    BatchAssignTenantRespVO batchAssignTenant(List<Long> deviceIds, Long tenantId);
}
