package com.enjoyiot.module.eiot.dal.dataobject.firewatch;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 火警处置工单 DO
 *
 * @author EnjoyIot
 */
@TableName("fire_alarm_order")
@KeySequence("fire_alarm_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FireAlarmOrderDO extends TenantBaseDO {

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
     * 物业ID
     */
    private Long propertyId;
    
    /**
     * 物业名称(冗余)
     */
    private String propertyName;
    
    /**
     * 设备ID
     */
    private Long deviceId;
    
    /**
     * 设备名称
     */
    private String deviceName;
    
    /**
     * 设备编码
     */
    private String deviceCode;
    
    /**
     * 告警类型: smoke-烟感, temperature-温感, manual-手动报警, composite-复合报警
     */
    private String alarmType;
    
    /**
     * 告警级别: critical-严重, major-较大, warning-一般, info-提示
     */
    private String alarmLevel;
    
    /**
     * 告警时间
     */
    private LocalDateTime alarmTime;
    
    /**
     * 告警位置
     */
    private String alarmLocation;
    
    /**
     * 告警描述
     */
    private String alarmDescription;
    
    /**
     * 告警数据(JSON格式)
     */
    private String alarmData;
    
    /**
     * AI分析结果(JSON格式)
     */
    private String aiAnalysis;
    
    /**
     * AI建议
     */
    private String aiSuggestion;
    
    /**
     * 联动视频URL
     */
    private String videoUrl;
    
    /**
     * 视频设备ID
     */
    private Long videoDeviceId;
    
    /**
     * 工单状态: pending-待处理, confirming-确认中, confirmed-已确认, 
     * false_alarm-误报, real_fire-真实火警, closed-已关闭
     */
    private String orderStatus;
    
    /**
     * 处理人
     */
    private String handler;
    
    /**
     * 处理时间
     */
    private LocalDateTime handleTime;
    
    /**
     * 处理类型: confirm-确认火警, false_alarm-误报, test-测试
     */
    private String handleType;
    
    /**
     * 处理备注
     */
    private String handleRemark;
    
    /**
     * 响应时长(秒)
     */
    private Integer responseDuration;
    
    /**
     * 处置时长(秒)
     */
    private Integer handleDuration;
    
    /**
     * 是否已通知
     */
    private Boolean isNotified;
    
    /**
     * 通知渠道(JSON数组)
     */
    private String notifyChannels;

}
