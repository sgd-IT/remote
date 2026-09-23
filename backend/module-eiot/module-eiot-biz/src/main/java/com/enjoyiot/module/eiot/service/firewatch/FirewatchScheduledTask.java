package com.enjoyiot.module.eiot.service.firewatch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enjoyiot.framework.tenant.core.aop.TenantIgnore;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireMaintenancePlanDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePatrolRecordDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePatrolTaskDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireRepairOrderDO;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePartolRecordMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePatrolTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消防模块通用定时任务
 * 统一管理消防值守模块的所有定时任务
 *
 * @author sgd
 */
@Slf4j
@Component
public class FirewatchScheduledTask {

    @Resource
    private FireRepairOrderService fireRepairOrderService;

    @Resource
    private FireMaintenancePlanService fireMaintenancePlanService;

    @Resource
    private FirePatrolTaskMapper firePatrolTaskMapper;

    @Resource
    private FirePartolRecordMapper firePartolRecordMapper;

    // ========================= 巡检任务 =========================

    /**
     * 每天 00:05 执行巡检任务状态管理：
     * 1. 标记超期任务
     * 2. 快照所有巡检任务到记录表
     * 3. 重置日常巡检任务状态为待执行
     */
    @Scheduled(cron = "0 5 0 * * ?")
    @TenantIgnore
    @Transactional(rollbackFor = Exception.class)
    public void refreshPatrolOverdueTasks() {
        log.info("[FirewatchScheduledTask] 开始执行巡检任务状态管理...");

        // 步骤1：将超期的 routine 任务标记为 overdue
        firePatrolTaskMapper.batchRefreshPartolTaskStatus();

        // 步骤2：将所有巡检任务快照到巡检记录表
        snapshotAllTasksToRecord();

        // 步骤3：重置 routine 任务状态为 pending
        firePatrolTaskMapper.batchResetRoutineTaskStatus();

        log.info("[FirewatchScheduledTask] 巡检任务状态管理流程执行完成");
    }

    /**
     * 将所有巡检任务快照到 fire_patrol_record 表作为历史记录
     */
    private void snapshotAllTasksToRecord() {
        List<FirePatrolTaskDO> allTasks = firePatrolTaskMapper.selectList(new LambdaQueryWrapper<>());

        if (allTasks.isEmpty()) {
            log.info("[FirewatchScheduledTask] 没有需要快照的巡检任务");
            return;
        }

        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (FirePatrolTaskDO task : allTasks) {
            FirePatrolRecordDO record = FirePatrolRecordDO.builder()
                    .tenantId(task.getTenantId())
                    .taskId(task.getId())
                    .taskNo(task.getTaskNo())
                    .propertyId(task.getPropertyId())
                    .propertyName(task.getPropertyName())
                    .taskName(task.getTaskName())
                    .taskType(task.getTaskType())
                    .planDate(task.getPlanDate())
                    .planStartTime(task.getPlanStartTime())
                    .planEndTime(task.getPlanEndTime())
                    // 任务级快照：设备字段置默认值
                    .deviceId(0L)
                    .patrolTime(now)
                    .patrolPerson(task.getPatrolUserName())
                    .patrolPersonId(task.getPatrolUserId())
                    .patrolResult("normal")
                    .remark(task.getRemark())
                    .build();
            firePartolRecordMapper.insert(record);
            count++;
        }
        log.info("[FirewatchScheduledTask] 步骤2-巡检记录快照完成，共快照 {} 条任务", count);
    }

    // ========================= 维保催办 =========================

    /**
     * 每天 9:00 执行自动催办
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void autoSendUrge() {
        log.info("[FirewatchScheduledTask] 开始执行自动催办...");

        try {
            // 获取尚未催办的超期项目
            List<FireRepairOrderDO> unurgedOrders =
                    fireRepairOrderService.getUnurgedOverdueOrderList();
            List<FireMaintenancePlanDO> unurgedPlans =
                    fireMaintenancePlanService.getUnurgedOverduePlanList();

            if (unurgedOrders.isEmpty() && unurgedPlans.isEmpty()) {
                log.info("[FirewatchScheduledTask] 没有需要催办的项目");
                return;
            }

            log.info("[FirewatchScheduledTask] 发现 {} 个超期工单、{} 个逾期计划待催办",
                    unurgedOrders.size(), unurgedPlans.size());

            // 执行催办发送
            Map<String, Object> result = fireRepairOrderService.sendUrgeNotifications(unurgedOrders, unurgedPlans);

            log.info("[FirewatchScheduledTask] 自动催办执行完成: 成功通知 {} 家企业, 失败 {} 家",
                    result.get("successCount"), result.get("failCount"));
        } catch (Exception e) {
            log.error("[FirewatchScheduledTask] 自动催办执行异常", e);
        }
    }
}
