
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
package com.enjoyiot.module.eiot.api.alert;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.api.alert.dto.AlertConfig;
import com.enjoyiot.module.eiot.api.alert.dto.AlertConfigPageReqVO;
import com.enjoyiot.module.eiot.api.alert.dto.Message;
import com.enjoyiot.module.eiot.controller.admin.channelconfig.vo.ChannelConfig;
import com.enjoyiot.module.eiot.controller.admin.channeltemplate.vo.ChannelTemplate;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.FireDutyScheduleRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.FirePropertyRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePropertyDO;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePropertyMapper;
import com.enjoyiot.module.eiot.service.alert.AlertConfigService;
import com.enjoyiot.module.eiot.service.alert.ChannelConfigService;
import com.enjoyiot.module.eiot.service.alert.ChannelTemplateService;
import com.enjoyiot.module.eiot.service.firewatch.FireDutyScheduleService;
import com.enjoyiot.module.eiot.service.firewatch.FirePropertyService;
import com.enjoyiot.module.system.api.user.AdminUserApi;
import com.enjoyiot.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AlertApiImpl implements AlertApi {

    @Resource
    private AlertConfigService alertConfigService;

    @Resource
    private ChannelTemplateService channelTemplateService;

    @Resource
    private ChannelConfigService channelConfigService;

    @Resource
    private FirePropertyMapper firePropertyMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private FirePropertyService firePropertyService;

    @Resource
    private FireDutyScheduleService fireDutyScheduleService;

    @Override
    public PageResult<AlertConfig> getAlertConfigPage(AlertConfigPageReqVO reqVO) {
        return alertConfigService.getAlertConfigPage(reqVO);
    }

    @Override
    public Message getNotifyMessage(AlertConfig alertConfig) {
        ChannelTemplate channelTemplate = channelTemplateService.getChannelTemplate(alertConfig.getMessageTemplateId());
        if (Objects.isNull(channelTemplate)) {
            return null;
        }
        Long channelConfigId = channelTemplate.getChannelConfigId();

        Message message = Message.builder()
                .content(channelTemplate.getContent())
                .templateCode(channelTemplate.getTemplateCode())
                .smsTemplateCode(channelTemplate.getSmsTemplateCode())
                .alertConfigId(alertConfig.getId())
                .tenantId(alertConfig.getTenantId())
                .build();

        if (channelConfigId != null) {
            ChannelConfig channelConfig = channelConfigService.getChannelConfig(channelTemplate.getChannelConfigId());
            if (Objects.isNull(channelConfig)) {
                log.warn("告警通道配置丢失");
                return null;
            }
            message.setChannelCode(channelConfig.getCode());
            message.setChannelConfig(channelConfig.getParam());
        }
        return message;
    }

    /**
     * 获取物业联系人手机号
     *
     * @param tenantId 租户ID
     * @return
     */
    @Override
    public String getPropertyContactPhone(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        // 通过 tenantId 查询企业信息，获取联系人手机号
        // 管理员(tenantId=1)可能关联多个企业，取所有企业的联系人
        // 企业端只关联自己的企业（MyBatis-Plus 租户拦截器会自动过滤）
        LambdaQueryWrapper<FirePropertyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FirePropertyDO::getTenantId, tenantId);
        List<FirePropertyDO> properties = firePropertyMapper.selectList(wrapper);

        if (properties == null || properties.isEmpty()) {
            log.warn("[AlertApi] 未找到租户对应的企业信息，tenantId: {}", tenantId);
            return null;
        }

        // 收集所有企业的联系人手机号，逗号分隔
        List<String> phones = properties.stream()
                .map(FirePropertyDO::getContactPhone)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        if (phones.isEmpty()) {
            log.warn("[AlertApi] 企业联系人手机号为空，tenantId: {}", tenantId);
            return null;
        }

        return String.join(",", phones);
    }

    @Override
    public List<String> getAlertPushPhones(Long alertConfigId) {
        List<String> phones = new ArrayList<>();
        if (alertConfigId == null) {
            return phones;
        }
        AlertConfig alertConfig = alertConfigService.getAlertConfig(alertConfigId);
        if (alertConfig == null || alertConfig.getTenantId() == null) {
            log.warn("[AlertApi] 报警配置不存在或缺少租户ID，alertConfigId: {}", alertConfigId);
            return phones;
        }
        Long tenantId = alertConfig.getTenantId();

        // 1. 读取报警配置绑定的【消息推送岗位】
        List<Long> pushPostIds = alertConfig.getPushPostIds();
        if (CollUtil.isNotEmpty(pushPostIds)) {
            // 2. 筛选同一租户下归属上述岗位、且开启【消息推送】的用户
            TenantUtils.execute(tenantId, () -> {
                List<AdminUserRespDTO> users = adminUserApi.getUserListByPostIds(pushPostIds);
                if (users == null) {
                    return;
                }
                for (AdminUserRespDTO user : users) {
                    if (!Boolean.TRUE.equals(user.getPushEnabled())) {
                        continue;
                    }
                    if (StrUtil.isNotBlank(user.getMobile()) && !phones.contains(user.getMobile())) {
                        phones.add(user.getMobile());
                    }
                }
            });
            log.info("[AlertApi] 消息推送岗位用户手机号，alertConfigId: {}, 岗位: {}, 手机号: {}",
                    alertConfigId, pushPostIds, phones);
        }

        // 3. 追加当前租户下当前时段生效的值班人员
        try {
            FirePropertyRespVO property = firePropertyService.getPropertyByTenantId(tenantId);
            if (property != null) {
                List<FireDutyScheduleRespVO> duties = fireDutyScheduleService.getCurrentDuty(property.getId());
                if (duties != null) {
                    for (FireDutyScheduleRespVO duty : duties) {
                        if (StrUtil.isNotBlank(duty.getDutyUserPhone()) && !phones.contains(duty.getDutyUserPhone())) {
                            phones.add(duty.getDutyUserPhone());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[AlertApi] 追加当前值班人员失败，alertConfigId: {}, tenantId: {}", alertConfigId, tenantId, e);
        }

        return phones;
    }

}
