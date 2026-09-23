package com.enjoyiot.module.eiot.dal.dataobject.firewatch;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 值班记录 DO
 *
 * 记录类型说明：
 * - patrol: 巡查记录
 * - alarm: 告警处理
 * - handover: 交接班
 * - other: 其他
 *
 * @author EnjoyIot
 */
@TableName("fire_duty_record")
@KeySequence("fire_duty_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FireDutyRecordDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 关联排班ID
     */
    private Long scheduleId;

    /**
     * 所属物业ID
     */
    private Long propertyId;

    /**
     * 所属物业名称(冗余)
     */
    private String propertyName;

    /**
     * 记录时间
     */
    private LocalDateTime recordTime;

    /**
     * 记录类型: patrol-巡查记录, alarm-告警处理, handover-交接班, other-其他
     */
    private String recordType;

    /**
     * 记录标题
     */
    private String title;

    /**
     * 记录内容
     */
    private String content;

    /**
     * 关联设备ID
     */
    private Long deviceId;

    /**
     * 关联设备名称
     */
    private String deviceName;

    /**
     * 关联工单ID
     */
    private Long orderId;

    /**
     * 记录人ID
     */
    private Long recorderId;

    /**
     * 记录人姓名
     */
    private String recorderName;

    /**
     * 附件(JSON数组)
     */
    private String attachments;

    /**
     * 状态: normal-正常, abnormal-异常
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

}
