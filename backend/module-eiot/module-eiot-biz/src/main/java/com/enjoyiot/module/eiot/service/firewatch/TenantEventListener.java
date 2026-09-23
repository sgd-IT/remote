package com.enjoyiot.module.eiot.service.firewatch;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enjoyiot.framework.tenant.core.aop.TenantIgnore;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePropertyDO;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePropertyMapper;
import com.enjoyiot.module.system.api.tenant.event.TenantCreatedEvent;
import com.enjoyiot.module.system.api.tenant.event.TenantUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 租户事件监听器
 * 监听租户创建/更新事件，自动同步数据到 fire_property 表（消防物业/企业信息）
 *
 * 字段映射：
 *   租户名称(tenantName)    → 物业名称(name)
 *   联系人(contactName)     → 联系人(contactName)
 *   联系电话(contactMobile) → 联系电话(contactPhone)
 *   客户类型(customerType)  → 物业类型(propertyType)
 *   详细地址(address)       → 详细地址(address)
 *   省份(province)          → 省份(province)
 *   城市(city)              → 城市(city)
 *   区县(district)          → 区县(district)
 *   经度(longitude)         → 经度(longitude)
 *   纬度(latitude)          → 纬度(latitude)
 *   风险等级(riskLevel)     → 风险等级(riskLevel)
 *   信用代码(creditCode)    → 信用代码(creditCode)
 *
 * @author EnjoyIot
 */
@Slf4j
@Component
public class TenantEventListener {

    @Resource
    private FirePropertyMapper firePropertyMapper;

    /**
     * 监听租户创建事件，自动在 fire_property 表创建对应的企业信息
     */
    @EventListener
    @Async
    @TenantIgnore
    public void onTenantCreated(TenantCreatedEvent event) {
        try {
            Long tenantId = event.getTenantId();
            String tenantName = event.getTenantName();

            log.info("[onTenantCreated] 收到租户创建事件，tenantId={}, tenantName={}, customerType={}, address={}",
                    tenantId, tenantName, event.getCustomerType(), event.getAddress());

            // 检查是否已存在该租户的物业记录（防止重复创建）
            LambdaQueryWrapper<FirePropertyDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FirePropertyDO::getTenantId, tenantId);
            wrapper.last("LIMIT 1");
            FirePropertyDO existing = firePropertyMapper.selectOne(wrapper);

            if (existing != null) {
                log.info("[onTenantCreated] 租户{}的物业记录已存在，跳过创建", tenantId);
                return;
            }

            // 在该租户上下文中创建物业记录
            TenantUtils.execute(tenantId, () -> {
                String code = "P" + System.currentTimeMillis() + IdUtil.randomUUID().substring(0, 4).toUpperCase();

                FirePropertyDO property = FirePropertyDO.builder()
                        .name(tenantName)
                        .code(code)
                        .contactName(event.getContactName())
                        .contactPhone(event.getContactMobile())
                        .propertyType(event.getCustomerType())
                        .address(event.getAddress())
                        .province(event.getProvince())
                        .city(event.getCity())
                        .district(event.getDistrict())
                        .longitude(event.getLongitude())
                        .latitude(event.getLatitude())
                        .riskLevel(event.getRiskLevel() != null ? event.getRiskLevel() : "normal")
                        .creditCode(event.getCreditCode())
                        .status(1)
                        .build();

                firePropertyMapper.insert(property);
                log.info("[onTenantCreated] 自动创建物业记录成功，tenantId={}, 物业名称={}, 物业类型={}, 地址={}",
                        tenantId, tenantName, event.getCustomerType(), event.getAddress());
            });
        } catch (Exception e) {
            log.error("[onTenantCreated] 处理租户创建事件失败，tenantId={}", event.getTenantId(), e);
        }
    }

    /**
     * 监听租户更新事件，自动同步更新 fire_property 表中对应的企业信息
     */
    @EventListener
    @Async
    @TenantIgnore
    public void onTenantUpdated(TenantUpdatedEvent event) {
        try {
            Long tenantId = event.getTenantId();

            log.info("[onTenantUpdated] 收到租户更新事件，tenantId={}, customerType={}, address={}",
                    tenantId, event.getCustomerType(), event.getAddress());

            // 查找该租户对应的物业记录
            LambdaQueryWrapper<FirePropertyDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FirePropertyDO::getTenantId, tenantId);
            wrapper.last("LIMIT 1");
            FirePropertyDO existing = firePropertyMapper.selectOne(wrapper);

            if (existing == null) {
                log.info("[onTenantUpdated] 租户{}没有物业记录，尝试创建", tenantId);
                // 如果不存在，则创建一条
                onTenantCreated(new TenantCreatedEvent(this, tenantId, event.getTenantName(),
                        event.getContactName(), event.getContactMobile(), event.getCustomerType(),
                        event.getAddress(), event.getProvince(), event.getCity(), event.getDistrict(),
                        event.getLongitude(), event.getLatitude(), event.getRiskLevel(), event.getCreditCode()));
                return;
            }

            // 更新物业信息
            TenantUtils.execute(tenantId, () -> {
                FirePropertyDO updateObj = FirePropertyDO.builder()
                        .id(existing.getId())
                        .name(event.getTenantName())
                        .contactName(event.getContactName())
                        .contactPhone(event.getContactMobile())
                        .propertyType(event.getCustomerType())
                        .address(event.getAddress())
                        .province(event.getProvince())
                        .city(event.getCity())
                        .district(event.getDistrict())
                        .longitude(event.getLongitude())
                        .latitude(event.getLatitude())
                        .riskLevel(event.getRiskLevel())
                        .creditCode(event.getCreditCode())
                        .build();

                firePropertyMapper.updateById(updateObj);
                log.info("[onTenantUpdated] 同步更新物业记录成功，tenantId={}, 物业名称={}, 物业类型={}, 地址={}",
                        tenantId, event.getTenantName(), event.getCustomerType(), event.getAddress());
            });
        } catch (Exception e) {
            log.error("[onTenantUpdated] 处理租户更新事件失败，tenantId={}", event.getTenantId(), e);
        }
    }
}
