package com.enjoyiot.module.eiot.service.firewatch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.maintenanceunit.MaintenanceUnitCreateReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.maintenanceunit.MaintenanceUnitPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.maintenanceunit.MaintenanceUnitRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.maintenanceunit.MaintenanceUnitUpdateReqVO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireMaintenanceUnitDO;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireMaintenanceUnitMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 维保单位管理 Service 实现类
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class FireMaintenanceUnitServiceImpl implements FireMaintenanceUnitService {

    @Resource
    private FireMaintenanceUnitMapper fireMaintenanceUnitMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUnit(MaintenanceUnitCreateReqVO reqVO) {
        FireMaintenanceUnitDO unit = FireMaintenanceUnitDO.builder()
                .unitName(reqVO.getUnitName())
                .contactPerson(reqVO.getContactPerson())
                .contactPhone(reqVO.getContactPhone())
                .address(reqVO.getAddress())
                .qualificationLevel(reqVO.getQualificationLevel())
                .remark(reqVO.getRemark())
                .build();

        unit.setTenantId(TenantContextHolder.getTenantId());
        fireMaintenanceUnitMapper.insert(unit);

        log.info("[createUnit] 创建维保单位成功，单位名称：{}", unit.getUnitName());
        return unit.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUnit(MaintenanceUnitUpdateReqVO reqVO) {
        tenantAware(() -> {
            FireMaintenanceUnitDO exist = fireMaintenanceUnitMapper.selectById(reqVO.getId());
            if (exist == null) {
                throw new IllegalArgumentException("维保单位不存在");
            }

            FireMaintenanceUnitDO updateObj = FireMaintenanceUnitDO.builder()
                    .id(reqVO.getId())
                    .unitName(reqVO.getUnitName())
                    .contactPerson(reqVO.getContactPerson())
                    .contactPhone(reqVO.getContactPhone())
                    .address(reqVO.getAddress())
                    .qualificationLevel(reqVO.getQualificationLevel())
                    .remark(reqVO.getRemark())
                    .build();

            fireMaintenanceUnitMapper.updateById(updateObj);

            log.info("[updateUnit] 更新维保单位成功，单位ID：{}", reqVO.getId());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUnit(Long id) {
        tenantAware(() -> {
            FireMaintenanceUnitDO exist = fireMaintenanceUnitMapper.selectById(id);
            if (exist == null) {
                throw new IllegalArgumentException("维保单位不存在");
            }
            fireMaintenanceUnitMapper.deleteById(id);
            log.info("[deleteUnit] 删除维保单位成功，单位ID：{}", id);
        });
    }

    @Override
    public MaintenanceUnitRespVO getUnit(Long id) {
        return tenantAware(() -> {
            FireMaintenanceUnitDO unit = fireMaintenanceUnitMapper.selectById(id);
            if (unit == null) {
                return null;
            }
            return convertToRespVO(unit);
        });
    }

    @Override
    public PageResult<MaintenanceUnitRespVO> pageUnits(MaintenanceUnitPageReqVO pageReqVO) {
        return tenantAware(() -> {
            LambdaQueryWrapper<FireMaintenanceUnitDO> wrapper = new LambdaQueryWrapper<>();

            wrapper.like(pageReqVO.getUnitName() != null && !pageReqVO.getUnitName().isEmpty(),
                    FireMaintenanceUnitDO::getUnitName, pageReqVO.getUnitName());
            wrapper.like(pageReqVO.getContactPerson() != null && !pageReqVO.getContactPerson().isEmpty(),
                    FireMaintenanceUnitDO::getContactPerson, pageReqVO.getContactPerson());
            wrapper.eq(pageReqVO.getQualificationLevel() != null && !pageReqVO.getQualificationLevel().isEmpty(),
                    FireMaintenanceUnitDO::getQualificationLevel, pageReqVO.getQualificationLevel());

            wrapper.orderByDesc(FireMaintenanceUnitDO::getCreateTime);

            PageResult<FireMaintenanceUnitDO> pageResult = fireMaintenanceUnitMapper.selectPage(pageReqVO, wrapper);

            List<MaintenanceUnitRespVO> list = new ArrayList<>();
            for (FireMaintenanceUnitDO unit : pageResult.getList()) {
                list.add(convertToRespVO(unit));
            }

            return new PageResult<>(list, pageResult.getTotal());
        });
    }

    @Override
    public List<MaintenanceUnitRespVO> listUnits() {
        return tenantAware(() -> {
            LambdaQueryWrapper<FireMaintenanceUnitDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(FireMaintenanceUnitDO::getCreateTime);
            List<FireMaintenanceUnitDO> units = fireMaintenanceUnitMapper.selectList(wrapper);

            List<MaintenanceUnitRespVO> list = new ArrayList<>();
            for (FireMaintenanceUnitDO unit : units) {
                list.add(convertToRespVO(unit));
            }
            return list;
        });
    }

    // ==================== 私有方法 ====================

    private boolean isSystemTenant() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId != null && tenantId == 1L;
    }

    private <T> T tenantAware(Supplier<T> supplier) {
        if (isSystemTenant()) {
            return TenantUtils.executeIgnoreWithResult(supplier::get);
        }
        return supplier.get();
    }

    /** 无返回值的 tenantAware，用于写操作 */
    private void tenantAware(Runnable runnable) {
        if (isSystemTenant()) {
            TenantUtils.executeIgnore(runnable);
        } else {
            runnable.run();
        }
    }

    private MaintenanceUnitRespVO convertToRespVO(FireMaintenanceUnitDO unit) {
        MaintenanceUnitRespVO vo = new MaintenanceUnitRespVO();
        vo.setId(unit.getId());
        vo.setUnitName(unit.getUnitName());
        vo.setContactPerson(unit.getContactPerson());
        vo.setContactPhone(unit.getContactPhone());
        vo.setAddress(unit.getAddress());
        vo.setQualificationLevel(unit.getQualificationLevel());
        vo.setRemark(unit.getRemark());
        vo.setCreateTime(unit.getCreateTime());
        return vo;
    }
}
