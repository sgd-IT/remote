package com.enjoyiot.module.eiot.dal.dataobject.firewatch;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 值班管理 DO
 *
 * 班次类型说明：
 * - day: 白班
 * - night: 夜班
 * - wholeday: 全天班
 *
 * @author EnjoyIot
 */
@TableName("fire_duty_schedule")
@KeySequence("fire_duty_schedule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FireDutyScheduleDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 所属物业ID
     */
    private Long propertyId;

    /**
     * 所属物业名称(冗余)
     */
    private String propertyName;

    /**
     * 排班日期
     */
    private LocalDate scheduleDate;

    /**
     * 班次类型: day-白班, night-夜班, wholeday-全天班
     */
    private String shiftType;

    /**
     * 班次名称
     */
    private String shiftName;

    /**
     * 开始时间(时分，格式: HH:mm)
     */
    private String startTime;

    /**
     * 结束时间(时分，格式: HH:mm)
     */
    private String endTime;

    /**
     * 值班人ID
     */
    private Long dutyUserId;

    /**
     * 值班人姓名
     */
    private String dutyUserName;

    /**
     * 值班人电话
     */
    private String dutyUserPhone;

    /**
     * 值班人岗位
     */
    private String dutyUserPosition;

    /**
     * 状态: scheduled-已排班, on_duty-值班中, completed-已完成, absent-缺勤, early_leave-早退
     */
    private String status;

    /**
     * 签到时间
     */
    private LocalDateTime checkInTime;

    /**
     * 签退时间
     */
    private LocalDateTime checkOutTime;

    /**
     * 签到方式: manual-手动, face-人脸, card-刷卡
     */
    private String checkInType;

    /**
     * 备注
     */
    private String remark;

}
