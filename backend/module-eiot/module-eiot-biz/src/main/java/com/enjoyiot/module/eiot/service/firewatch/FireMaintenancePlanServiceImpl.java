package com.enjoyiot.module.eiot.service.firewatch;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.tenant.core.aop.TenantIgnore;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan.PlanCreateReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan.PlanPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan.PlanRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan.PlanUpdateReqVO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireMaintenancePlanDO;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireMaintenancePlanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 维保计划管理 Service 实现类
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class FireMaintenancePlanServiceImpl implements FireMaintenancePlanService {

    @Resource
    private FireMaintenancePlanMapper fireMaintenancePlanMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPlan(PlanCreateReqVO reqVO) {
        log.info("[createPlan] ===== 开始创建维保计划 ===== 请求参数: planNo={}, planName={}, propertyId={}, propertyName={}, planType={}, planDate={}, executeUnit={}, executePerson={}, remark={}",
                reqVO.getPlanNo(), reqVO.getPlanName(), reqVO.getPropertyId(), reqVO.getPropertyName(),
                reqVO.getPlanType(), reqVO.getPlanDate(), reqVO.getExecuteUnit(), reqVO.getExecutePerson(), reqVO.getRemark());
        try {
            FireMaintenancePlanDO plan = FireMaintenancePlanDO.builder()
                    .planNo(generatePlanNo())
                    .planName(reqVO.getPlanName())
                    .propertyId(reqVO.getPropertyId())
                    .propertyName(reqVO.getPropertyName())
                    .planType(reqVO.getPlanType())
                    .planDate(reqVO.getPlanDate())
                    .executePerson(reqVO.getExecutePerson())
                    .executeUnit(reqVO.getExecuteUnit())
                    .planStatus("pending")
                    .remark(reqVO.getRemark())
                    .build();

            Long tenantId = TenantContextHolder.getTenantId();
            log.info("[createPlan] 租户ID: {}, 待插入数据: {}", tenantId, plan);
            plan.setTenantId(tenantId);

            int insertResult = fireMaintenancePlanMapper.insert(plan);
            log.info("[createPlan] 数据库insert结果: {}, 生成ID: {}, 计划编号: {}", insertResult, plan.getId(), plan.getPlanNo());

            return plan.getId();
        } catch (Exception e) {
            log.error("[createPlan] ===== 创建维保计划异常 ===== reqVO={}", reqVO, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlan(PlanUpdateReqVO reqVO) {
        tenantAware(() -> {
            FireMaintenancePlanDO existPlan = fireMaintenancePlanMapper.selectById(reqVO.getId());
            if (existPlan == null) {
                throw new IllegalArgumentException("维保计划不存在");
            }

            FireMaintenancePlanDO updateObj = FireMaintenancePlanDO.builder()
                    .id(reqVO.getId())
                    .planNo(reqVO.getPlanNo())
                    .planName(reqVO.getPlanName())
                    .propertyId(reqVO.getPropertyId())
                    .propertyName(reqVO.getPropertyName())
                    .planType(reqVO.getPlanType())
                    .planDate(reqVO.getPlanDate())
                    .executePerson(reqVO.getExecutePerson())
                    .executeUnit(reqVO.getExecuteUnit())
                    .remark(reqVO.getRemark())
                    .build();

            fireMaintenancePlanMapper.updateById(updateObj);

            log.info("[updatePlan] 更新维保计划成功，计划ID：{}", reqVO.getId());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Long id) {
        tenantAware(() -> {
            FireMaintenancePlanDO existPlan = fireMaintenancePlanMapper.selectById(id);
            if (existPlan == null) {
                throw new IllegalArgumentException("维保计划不存在");
            }

            fireMaintenancePlanMapper.deleteById(id);

            log.info("[deletePlan] 删除维保计划成功，计划ID：{}", id);
        });
    }

    @Override
    public PlanRespVO getPlan(Long id) {
        return tenantAware(() -> {
            FireMaintenancePlanDO plan = fireMaintenancePlanMapper.selectById(id);
            if (plan == null) {
                return null;
            }
            return convertToRespVO(plan);
        });
    }

    @Override
    public PageResult<PlanRespVO> pagePlans(PlanPageReqVO pageReqVO) {
        return tenantAware(() -> {
            LambdaQueryWrapper<FireMaintenancePlanDO> wrapper = new LambdaQueryWrapper<>();

            wrapper.like(pageReqVO.getPlanNo() != null, FireMaintenancePlanDO::getPlanNo, pageReqVO.getPlanNo());
            wrapper.like(pageReqVO.getPlanName() != null, FireMaintenancePlanDO::getPlanName, pageReqVO.getPlanName());
            wrapper.eq(pageReqVO.getPlanType() != null, FireMaintenancePlanDO::getPlanType, pageReqVO.getPlanType());
            wrapper.eq(pageReqVO.getPlanStatus() != null, FireMaintenancePlanDO::getPlanStatus, pageReqVO.getPlanStatus());
            wrapper.like(pageReqVO.getExecuteUnit() != null, FireMaintenancePlanDO::getExecuteUnit, pageReqVO.getExecuteUnit());
            wrapper.like(pageReqVO.getPropertyName() != null, FireMaintenancePlanDO::getPropertyName, pageReqVO.getPropertyName());

            wrapper.orderByDesc(FireMaintenancePlanDO::getCreateTime);

            PageResult<FireMaintenancePlanDO> pageResult = fireMaintenancePlanMapper.selectPage(pageReqVO, wrapper);

            List<PlanRespVO> list = new ArrayList<>();
            for (FireMaintenancePlanDO plan : pageResult.getList()) {
                list.add(convertToRespVO(plan));
            }

            return new PageResult<>(list, pageResult.getTotal());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completePlan(Long id) {
        tenantAware(() -> {
            FireMaintenancePlanDO existPlan = fireMaintenancePlanMapper.selectById(id);
            if (existPlan == null) {
                throw new IllegalArgumentException("维保计划不存在");
            }
            if ("completed".equals(existPlan.getPlanStatus())) {
                throw new IllegalArgumentException("维保计划已处于完成状态");
            }

            fireMaintenancePlanMapper.update(null, new LambdaUpdateWrapper<FireMaintenancePlanDO>()
                    .eq(FireMaintenancePlanDO::getId, id)
                    .set(FireMaintenancePlanDO::getPlanStatus, "completed"));

            log.info("[completePlan] 完成维保计划，计划ID：{}", id);
        });
    }

    @Override
    public List<PlanRespVO> getOverduePlans() {
        return tenantAware(() -> {
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            List<FireMaintenancePlanDO> overduePlans = fireMaintenancePlanMapper.selectOverduePlans(tenantId);

            List<PlanRespVO> list = new ArrayList<>();
            for (FireMaintenancePlanDO plan : overduePlans) {
                list.add(convertToRespVO(plan));
            }
            return list;
        });
    }

    @Override
    public Integer countOverdue() {
        return tenantAware(() -> {
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            return fireMaintenancePlanMapper.countOverduePlans(tenantId);
        });
    }

    @Override
    public List<FireMaintenancePlanDO> getOverduePlanList() {
        return tenantAware(() -> {
            if (isSystemTenant()) {
                return fireMaintenancePlanMapper.selectAllOverduePlans();
            } else {
                Long tenantId = TenantContextHolder.getRequiredTenantId();
                return fireMaintenancePlanMapper.selectOverduePlans(tenantId);
            }
        });
    }

    @Override
    public List<FireMaintenancePlanDO> getUnurgedOverduePlanList() {
        List<FireMaintenancePlanDO> allOverdue = getOverduePlanList();
        return allOverdue.stream()
                .filter(p -> p.getUrgeSent() == null || !p.getUrgeSent())
                .collect(Collectors.toList());
    }

    @Override
    @TenantIgnore
    public void markUrgeSent(List<Long> planIds) {
        if (CollUtil.isEmpty(planIds)) return;
        fireMaintenancePlanMapper.batchMarkUrgeSent(planIds);
    }

    @Override
    public Integer countUnurgedOverduePlans() {
        return getUnurgedOverduePlanList().size();
    }

    // ==================== 私有方法 ====================

    /**
     * 生成计划编号：PL-yyyyMMdd-xxxx
     */
    private String generatePlanNo() {
        String datePart = java.time.LocalDate.now().toString().replace("-", "");
        String randomPart = IdUtil.randomUUID().substring(0, 4).toUpperCase();
        return "PL-" + datePart + "-" + randomPart;
    }

    /**
     * 获取计划类型名称
     */
    private String getPlanTypeName(String planType) {
        Map<String, String> map = new HashMap<>();
        map.put("monthly", "月度");
        map.put("quarterly", "季度");
        map.put("yearly", "年度");
        return map.getOrDefault(planType, planType);
    }

    /**
     * 获取计划状态名称
     */
    private String getPlanStatusName(String planStatus) {
        Map<String, String> map = new HashMap<>();
        map.put("pending", "待执行");
        map.put("executing", "执行中");
        map.put("completed", "已完成");
        map.put("overdue", "已逾期");
        return map.getOrDefault(planStatus, planStatus);
    }

    /**
     * 转换为响应VO
     */
    private PlanRespVO convertToRespVO(FireMaintenancePlanDO plan) {
        PlanRespVO vo = new PlanRespVO();
        vo.setId(plan.getId());
        vo.setPlanNo(plan.getPlanNo());
        vo.setPlanName(plan.getPlanName());
        vo.setPropertyId(plan.getPropertyId());
        vo.setPropertyName(plan.getPropertyName());
        vo.setPlanType(plan.getPlanType());
        vo.setPlanTypeName(getPlanTypeName(plan.getPlanType()));
        vo.setPlanDate(plan.getPlanDate());
        vo.setExecutePerson(plan.getExecutePerson());
        vo.setExecuteUnit(plan.getExecuteUnit());
        vo.setPlanStatus(plan.getPlanStatus());
        vo.setPlanStatusName(getPlanStatusName(plan.getPlanStatus()));
        vo.setRemark(plan.getRemark());
        vo.setCreateTime(plan.getCreateTime());
        return vo;
    }

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
}
