package com.enjoyiot.module.eiot.dal.dataobject.firewatch;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 巡检任务 DO
 *
 * 任务状态说明：
 * - pending: 待执行
 * - in_progress: 进行中
 * - completed: 已完成
 * - overdue: 已超期
 * - cancelled: 已取消
 *
 * @author EnjoyIot
 */
@TableName("fire_patrol_task")
@KeySequence("fire_patrol_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirePatrolTaskDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 任务编号
     */
    private String taskNo;

    /**
     * 所属物业ID
     */
    private Long propertyId;

    /**
     * 所属物业名称(冗余)
     */
    private String propertyName;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务类型: routine-日常巡检, special-专项检查, maintenance-维保检查
     */
    private String taskType;

    /**
     * 计划日期，null表示“每天”重复任务
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate planDate;

    /**
     * 计划开始时间
     */
    private LocalDateTime planStartTime;

    /**
     * 计划结束时间
     */
    private LocalDateTime planEndTime;

    /**
     * 实际开始时间
     */
    private LocalDateTime actualStartTime;

    /**
     * 实际结束时间
     */
    private LocalDateTime actualEndTime;

    /**
     * 任务状态: pending-待执行, in_progress-进行中, completed-已完成, overdue-已超期, cancelled-已取消
     */
    private String status;

    /**
     * 巡检人ID
     */
    private Long patrolUserId;

    /**
     * 巡检人姓名
     */
    private String patrolUserName;

    /**
     * 巡检人电话
     */
    private String patrolUserPhone;

    /**
     * 巡检点位数量
     */
    private Integer totalPoints;

    /**
     * 已完成点位数量
     */
    private Integer completedPoints;

    /**
     * 正常点位数量
     */
    private Integer normalPoints;

    /**
     * 异常点位数量
     */
    private Integer abnormalPoints;

    /**
     * 完成进度(%)
     */
    private Integer progress;

    /**
     * 巡检结果: normal-正常, abnormal-异常
     */
    private String result;

    /**
     * 巡检总结
     */
    private String summary;

    /**
     * 备注
     */
    private String remark;

}
