package com.enjoyiot.module.eiot.dal.dataobject.firewatch;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 巡检记录
 */
@TableName("fire_patrol_record")
@KeySequence("fire_patrol_record")
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirePatrolRecordDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 关联任务ID
     */
    private Long taskId;

    /**
     * 任务编号
     */
    private String taskNo;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 设备编码
     */
    private String deviceCode;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 所属物业ID
     */
    private Long propertyId;

    /**
     * 所属物业名称(冗余)
     */
    private String propertyName;

    /**
     * 巡检时间
     */
    private LocalDateTime patrolTime;

    /**
     * 巡检人姓名
     */
    private String patrolPerson;

    /**
     * 巡检人ID
     */
    private Long patrolPersonId;

    /**
     * 设备状态
     */
    private String deviceStatus;

    /**
     * 巡检结果: normal-正常, abnormal-异常
     */
    private String patrolResult;

    /**
     * 检查项(JSON)
     */
    private String checkItems;

    /**
     * 异常描述
     */
    private String abnormalDescription;

    /**
     * 异常图片(JSON数组)
     */
    private String abnormalImages;

    /**
     * 是否报修: 0-否, 1-是
     */
    private Integer isReportRepair;

    /**
     * 关联维修工单ID
     */
    private Long repairOrderId;

    /**
     * 位置经度
     */
    private BigDecimal locationLongitude;

    /**
     * 位置纬度
     */
    private BigDecimal locationLatitude;

    /**
     * 备注
     */
    private String remark;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务类型: routine-日常巡检, special-专项检查, maintenance-维保检查（后扩展字段）
     */
    private String taskType;

    /**
     * 计划日期
     */
    private LocalDate planDate;

    /**
     * 计划开始时间
     */
    private LocalDateTime planStartTime;

    /**
     * 计划结束时间
     */
    private LocalDateTime planEndTime;
}
