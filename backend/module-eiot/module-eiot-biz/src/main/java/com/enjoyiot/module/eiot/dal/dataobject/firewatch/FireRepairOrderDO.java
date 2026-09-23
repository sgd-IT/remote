package com.enjoyiot.module.eiot.dal.dataobject.firewatch;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 维修工单 DO
 *
 * 工单状态说明：
 * - pending: 待处理
 * - assigned: 已派单(待排查)
 * - completed: 已完成
 * - closed: 已关闭
 *
 * @author EnjoyIot
 */
@TableName("fire_repair_order")
@KeySequence("fire_repair_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FireRepairOrderDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 工单编号
     */
    private String orderNo;

    /**
     * 关联告警记录ID
     */
    private Long alertRecordId;

    /**
     * 来源: auto-alert-故障告警自动生成, manual-手动创建
     */
    private String source;

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
     * 物业ID
     */
    private Long propertyId;

    /**
     * 物业名称
     */
    private String propertyName;

    /**
     * 故障类型
     */
    private String faultType;

    /**
     * 故障描述
     */
    private String faultDescription;

    /**
     * 故障图片(JSON数组)
     */
    private String faultImages;

    /**
     * 报修人
     */
    private String reporter;

    /**
     * 报修人电话
     */
    private String reporterPhone;

    /**
     * 报修时间
     */
    private LocalDateTime reportTime;

    /**
     * 紧急程度: urgent-紧急, major-较大, normal-一般
     */
    private String urgencyLevel;

    /**
     * 工单状态: pending-待处理, assigned-已派单, completed-已完成, closed-已关闭
     */
    private String orderStatus;

    /**
     * 指派人(企业联系人)
     */
    private String assignee;

    /**
     * 指派人电话
     */
    private String assigneePhone;

    /**
     * 派单时间
     */
    private LocalDateTime assignTime;

    /**
     * 排查截止时间
     */
    private LocalDateTime deadline;

    /**
     * 签单时间
     */
    private LocalDateTime signTime;

    /**
     * 维保人
     */
    private String repairPerson;

    /**
     * 维保人电话
     */
    private String repairPersonPhone;

    /**
     * 维保单位
     */
    private String repairCompany;

    /**
     * 报修来源: platform-平台推送, phone-电话通知, manual-手动登记
     */
    private String reportSource;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 完成备注
     */
    private String completeRemark;

    /**
     * 完成图片(JSON数组)
     */
    private String completeImages;

    /**
     * 满意度评分(1-5)
     */
    private Integer satisfaction;

    /**
     * 评价建议
     */
    private String suggestion;

    /**
     * 是否已发送催促通知
     */
    private Boolean urgeSent;

    /**
     * 催促发送时间
     */
    private LocalDateTime urgeTime;
}
