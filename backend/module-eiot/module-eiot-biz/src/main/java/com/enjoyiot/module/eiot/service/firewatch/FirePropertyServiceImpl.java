package com.enjoyiot.module.eiot.service.firewatch;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePropertyDO;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePropertyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 物业单位管理 Service 实现类
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class FirePropertyServiceImpl implements FirePropertyService {

    @Resource
    private FirePropertyMapper firePropertyMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProperty(FirePropertyCreateReqVO createReqVO) {
        // 生成物业编码
        String code = createReqVO.getCode();
        if (code == null || code.isEmpty()) {
            code = "P" + System.currentTimeMillis() + IdUtil.randomUUID().substring(0, 4).toUpperCase();
        }
        
        FirePropertyDO property = FirePropertyDO.builder()
                .name(createReqVO.getName())
                .code(code)
                .address(createReqVO.getAddress())
                .province(createReqVO.getProvince())
                .city(createReqVO.getCity())
                .district(createReqVO.getDistrict())
                .longitude(createReqVO.getLongitude())
                .latitude(createReqVO.getLatitude())
                .contactName(createReqVO.getContactName())
                .contactPhone(createReqVO.getContactPhone())
                .riskLevel(createReqVO.getRiskLevel() != null ? createReqVO.getRiskLevel() : "normal")
                .propertyType(createReqVO.getPropertyType())
                .creditCode(createReqVO.getCreditCode())
                .customerType(createReqVO.getCustomerType())
                .floorCount(createReqVO.getFloorCount())
                .buildingArea(createReqVO.getBuildingArea())
                .mainPerson(createReqVO.getMainPerson())
                .safetyPerson(createReqVO.getSafetyPerson())
                .buildingFloors(createReqVO.getBuildingFloors())
                .licenseNo(createReqVO.getLicenseNo())
                .licenseAuthority(createReqVO.getLicenseAuthority())
                .licenseStartDate(createReqVO.getLicenseStartDate())
                .licenseEndDate(createReqVO.getLicenseEndDate())
                .licenseScope(createReqVO.getLicenseScope())
                .storageType(createReqVO.getStorageType())
                .storageArea(createReqVO.getStorageArea())
                .storageCapacity(createReqVO.getStorageCapacity())
                .storageGoodsType(createReqVO.getStorageGoodsType())
                .remark(createReqVO.getRemark())
                .status(createReqVO.getStatus() != null ? createReqVO.getStatus() : 1)
                .build();
        
        firePropertyMapper.insert(property);
        
        log.info("[createProperty] 创建物业成功，物业名称：{}，编码：{}", property.getName(), property.getCode());
        
        return property.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProperty(FirePropertyUpdateReqVO updateReqVO) {
        FirePropertyDO existProperty = firePropertyMapper.selectById(updateReqVO.getId());
        if (existProperty == null) {
            throw new IllegalArgumentException("物业不存在");
        }
        
        FirePropertyDO updateObj = FirePropertyDO.builder()
                .id(updateReqVO.getId())
                .name(updateReqVO.getName())
                .code(updateReqVO.getCode())
                .address(updateReqVO.getAddress())
                .province(updateReqVO.getProvince())
                .city(updateReqVO.getCity())
                .district(updateReqVO.getDistrict())
                .longitude(updateReqVO.getLongitude())
                .latitude(updateReqVO.getLatitude())
                .contactName(updateReqVO.getContactName())
                .contactPhone(updateReqVO.getContactPhone())
                .riskLevel(updateReqVO.getRiskLevel())
                .propertyType(updateReqVO.getPropertyType())
                .creditCode(updateReqVO.getCreditCode())
                .customerType(updateReqVO.getCustomerType())
                .floorCount(updateReqVO.getFloorCount())
                .buildingArea(updateReqVO.getBuildingArea())
                .mainPerson(updateReqVO.getMainPerson())
                .safetyPerson(updateReqVO.getSafetyPerson())
                .buildingFloors(updateReqVO.getBuildingFloors())
                .licenseNo(updateReqVO.getLicenseNo())
                .licenseAuthority(updateReqVO.getLicenseAuthority())
                .licenseStartDate(updateReqVO.getLicenseStartDate())
                .licenseEndDate(updateReqVO.getLicenseEndDate())
                .licenseScope(updateReqVO.getLicenseScope())
                .storageType(updateReqVO.getStorageType())
                .storageArea(updateReqVO.getStorageArea())
                .storageCapacity(updateReqVO.getStorageCapacity())
                .storageGoodsType(updateReqVO.getStorageGoodsType())
                .remark(updateReqVO.getRemark())
                .status(updateReqVO.getStatus())
                .build();
        
        firePropertyMapper.updateById(updateObj);
        
        log.info("[updateProperty] 更新物业成功，物业ID：{}", updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProperty(Long id) {
        FirePropertyDO existProperty = firePropertyMapper.selectById(id);
        if (existProperty == null) {
            throw new IllegalArgumentException("物业不存在");
        }
        
        firePropertyMapper.deleteById(id);
        
        log.info("[deleteProperty] 删除物业成功，物业ID：{}", id);
    }

    @Override
    public FirePropertyRespVO getProperty(Long id) {
        // admin可查看任意物业，企业只能查看自己租户的物业（由租户拦截器自动过滤）
        if (isSystemTenant()) {
            return TenantUtils.executeIgnoreWithResult(() -> {
                FirePropertyDO property = firePropertyMapper.selectById(id);
                return property != null ? convertToRespVO(property) : null;
            });
        }
        FirePropertyDO property = firePropertyMapper.selectById(id);
        if (property == null) {
            return null;
        }
        return convertToRespVO(property);
    }

    @Override
    public PageResult<FirePropertyRespVO> pageProperties(FirePropertyPageReqVO pageReqVO) {
        // admin看全部租户数据，企业只看自己的（由MyBatis-Plus租户拦截器自动过滤）
        if (isSystemTenant()) {
            return TenantUtils.executeIgnoreWithResult(() -> doPageProperties(pageReqVO));
        }
        return doPageProperties(pageReqVO);
    }

    private PageResult<FirePropertyRespVO> doPageProperties(FirePropertyPageReqVO pageReqVO) {
        log.info("[pageProperties] 收到查询请求: pageNo={}, pageSize={}, name={}, code={}, riskLevel={}, propertyType={}, status={}",
                pageReqVO.getPageNo(), pageReqVO.getPageSize(),
                pageReqVO.getName(), pageReqVO.getCode(),
                pageReqVO.getRiskLevel(), pageReqVO.getPropertyType(),
                pageReqVO.getStatus());

        LambdaQueryWrapper<FirePropertyDO> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.like(pageReqVO.getName() != null && !pageReqVO.getName().isEmpty(), FirePropertyDO::getName, pageReqVO.getName());
        wrapper.eq(pageReqVO.getCode() != null && !pageReqVO.getCode().isEmpty(), FirePropertyDO::getCode, pageReqVO.getCode());
        wrapper.eq(pageReqVO.getProvince() != null && !pageReqVO.getProvince().isEmpty(), FirePropertyDO::getProvince, pageReqVO.getProvince());
        wrapper.eq(pageReqVO.getCity() != null && !pageReqVO.getCity().isEmpty(), FirePropertyDO::getCity, pageReqVO.getCity());
        wrapper.eq(pageReqVO.getDistrict() != null && !pageReqVO.getDistrict().isEmpty(), FirePropertyDO::getDistrict, pageReqVO.getDistrict());
        wrapper.eq(pageReqVO.getRiskLevel() != null && !pageReqVO.getRiskLevel().isEmpty(), FirePropertyDO::getRiskLevel, pageReqVO.getRiskLevel());
        wrapper.eq(pageReqVO.getPropertyType() != null && !pageReqVO.getPropertyType().isEmpty(), FirePropertyDO::getPropertyType, pageReqVO.getPropertyType());
        wrapper.eq(pageReqVO.getStatus() != null, FirePropertyDO::getStatus, pageReqVO.getStatus());
        
        wrapper.orderByDesc(FirePropertyDO::getCreateTime);
        
        PageResult<FirePropertyDO> pageResult = firePropertyMapper.selectPage(pageReqVO, wrapper);
        
        List<FirePropertyRespVO> list = new ArrayList<>();
        for (FirePropertyDO property : pageResult.getList()) {
            list.add(convertToRespVO(property));
        }
        
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public List<FirePropertyRespVO> listProperties(Integer status) {
        LambdaQueryWrapper<FirePropertyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null && status > 0, FirePropertyDO::getStatus, status);
        wrapper.orderByDesc(FirePropertyDO::getCreateTime);

        List<FirePropertyDO> properties;
        Long tenantId = TenantContextHolder.getTenantId();
        // 系统管理员（tenantId=1）查看全部物业，普通租户只查看自己的
        if (tenantId != null && tenantId == 1L) {
            properties = TenantUtils.executeIgnoreWithResult(() ->
                    firePropertyMapper.selectList(wrapper));
        } else {
            properties = firePropertyMapper.selectList(wrapper);
        }

        List<FirePropertyRespVO> list = new ArrayList<>();
        for (FirePropertyDO property : properties) {
            list.add(convertToRespVO(property));
        }

        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        FirePropertyDO property = firePropertyMapper.selectById(id);
        if (property == null) {
            throw new IllegalArgumentException("物业不存在");
        }
        
        firePropertyMapper.update(null, new LambdaUpdateWrapper<FirePropertyDO>()
                .eq(FirePropertyDO::getId, id)
                .set(FirePropertyDO::getStatus, status));
        
        log.info("[updateStatus] 更新物业状态，物业ID：{}，状态：{}", id, status);
    }

    @Override
    public FirePropertyRespVO getPropertyStatistics(Long id) {
        FirePropertyDO property = firePropertyMapper.selectById(id);
        if (property == null) {
            return null;
        }
        
        FirePropertyRespVO vo = convertToRespVO(property);
        // TODO: 统计设备数量
        vo.setDeviceCount(0);
        
        return vo;
    }

    @Override
    public FirePropertyRespVO getPropertyByTenantId(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        LambdaQueryWrapper<FirePropertyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FirePropertyDO::getTenantId, tenantId);
        wrapper.last("LIMIT 1");
        FirePropertyDO property = firePropertyMapper.selectOne(wrapper);
        if (property == null) {
            return null;
        }
        return convertToRespVO(property);
    }



    /**
     * 判断当前是否为系统租户（tenant_id=1，即admin）
     */
    private boolean isSystemTenant() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId != null && tenantId == 1L;
    }

    /**
     * 获取风险等级名称
     */
    private String getRiskLevelName(String riskLevel) {
        Map<String, String> map = new HashMap<>();
        map.put("critical", "重大风险");
        map.put("major", "较大风险");
        map.put("normal", "一般风险");
        return map.getOrDefault(riskLevel, riskLevel);
    }

    /**
     * 转换为响应VO
     */
    private FirePropertyRespVO convertToRespVO(FirePropertyDO property) {
        FirePropertyRespVO vo = new FirePropertyRespVO();
        vo.setId(property.getId());
        vo.setTenantId(property.getTenantId());
        vo.setName(property.getName());
        vo.setPropertyName(property.getName());
        vo.setCode(property.getCode());
        vo.setAddress(property.getAddress());
        vo.setProvince(property.getProvince());
        vo.setCity(property.getCity());
        vo.setDistrict(property.getDistrict());
        vo.setLongitude(property.getLongitude());
        vo.setLatitude(property.getLatitude());
        vo.setContactName(property.getContactName());
        vo.setContactPhone(property.getContactPhone());
        vo.setRiskLevel(property.getRiskLevel());
        vo.setRiskLevelName(getRiskLevelName(property.getRiskLevel()));
        vo.setPropertyType(property.getPropertyType());
        vo.setCreditCode(property.getCreditCode());
        vo.setCustomerType(property.getCustomerType());
        vo.setFloorCount(property.getFloorCount());
        vo.setBuildingArea(property.getBuildingArea());
        vo.setMainPerson(property.getMainPerson());
        vo.setSafetyPerson(property.getSafetyPerson());
        vo.setBuildingFloors(property.getBuildingFloors());
        vo.setLicenseNo(property.getLicenseNo());
        vo.setLicenseAuthority(property.getLicenseAuthority());
        vo.setLicenseStartDate(property.getLicenseStartDate());
        vo.setLicenseEndDate(property.getLicenseEndDate());
        vo.setLicenseScope(property.getLicenseScope());
        vo.setStorageType(property.getStorageType());
        vo.setStorageArea(property.getStorageArea());
        vo.setStorageCapacity(property.getStorageCapacity());
        vo.setStorageGoodsType(property.getStorageGoodsType());
        vo.setRemark(property.getRemark());
        vo.setStatus(property.getStatus());
        vo.setCreateTime(property.getCreateTime());
        return vo;
    }

}
