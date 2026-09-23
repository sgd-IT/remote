package com.enjoyiot.module.eiot.dal.dataobject.firewatch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 隐患管理 DO
 *
 * 隐患状态说明：
 * - pending: 待整改
 * - rectifying: 整改中
 * - reviewing: 待验收
 * - closed: 已闭环
 * - cancelled: 已关闭
 *
 * 隐患等级说明：
 * - general: 一般隐患
 * - major: 较大隐患
 * - critical: 重大隐患
 *
 * @author EnjoyIot
 */
@TableName("fire_hazard")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FireHazardDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 隐患编号
     */
    private String hazardNo;

    /**
     * 所属物业ID
     */
    private Long propertyId;

    /**
     * 所属物业名称(冗余)
     */
    private String propertyName;

    /**
     * 关联设备ID
     */
    private Long deviceId;

    /**
     * 关联设备名称(冗余)
     */
    private String deviceName;

    /**
     * 隐患等级: general-一般隐患, major-较大隐患, critical-重大隐患
     */
    private String hazardLevel;

    /**
     * 隐患描述
     */
    private String hazardDesc;

    /**
     * 隐患位置
     */
    private String location;

    /**
     * 发现人
     */
    private String foundUserName;

    /**
     * 发现日期
     */
    private LocalDate foundDate;

    /**
     * 整改截止日期
     */
    private LocalDate deadline;

    /**
     * 整改建议
     */
    private String suggestion;

    /**
     * 现场照片(JSON数组)
     */
    private String photos;

    /**
     * 隐患状态: pending-待整改, rectifying-整改中, reviewing-待验收, closed-已闭环, cancelled-已关闭
     */
    private String status;

    /**
     * 整改人ID
     */
    private Long rectificationUserId;

    /**
     * 整改人姓名
     */
    private String rectificationUserName;

    /**
     * 整改措施
     */
    private String measure;

    /**
     * 整改进度(%)
     */
    private Integer rectificationProgress;

    /**
     * 整改记录(JSON数组)
     */
    private String rectificationRecords;

    /**
     * 验收结果: approved-通过, rejected-不通过
     */
    private String reviewResult;

    /**
     * 验收意见
     */
    private String reviewRemark;

    /**
     * 验收时间
     */
    private LocalDateTime reviewTime;

    /**
     * 关闭原因
     */
    private String closeRemark;

    /**
     * 备注
     */
    private String remark;

}
