package com.enjoyiot.module.eiot.dal.dataobject.firewatch;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 话单通话记录 DO
 * <p>
 * 字段与《话单推送接口文档 V2.6》1.1.2 发送参数说明一一对应，
 * 并扩展 tenantId（号码匹配归属租户）、matchPhone（匹配到的座机号码）、
 * callDirection（通话方向）三个字段。
 *
 * @author EnjoyIot
 */
@TableName("eiot_call_record")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FireCallRecordDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 通话唯一标志（话单 sessionid，幂等键）
     */
    private String sessionId;

    /**
     * 显示号码
     */
    private String displayNbr;

    /**
     * 主叫号码
     */
    private String callerNbr;

    /**
     * 被叫号码
     */
    private String calledNbr;

    /**
     * 业务号码
     */
    private String serviceNbr;

    /**
     * 业务号码B
     */
    private String serviceNbrB;

    /**
     * 拨打时间(YYYYMMDDHH24MISS)
     */
    private String dialTime;

    /**
     * 振铃时间(YYYYMMDDHH24MISS)
     */
    private String ringTime;

    /**
     * 开始时间(YYYYMMDDHH24MISS)
     */
    private String startTime;

    /**
     * 结束时间(YYYYMMDDHH24MISS)
     */
    private String endTime;

    /**
     * 通话时长(秒)
     */
    private String duration;

    /**
     * 通话时长(分钟)
     */
    private String minDuration;

    /**
     * 话单类型(yly-caller/yly-callee/yzj-caller/yzj-callee/recyzj-caller/recyzj-callee/gzh-caller/gzh-callee)
     */
    private String serviceType;

    /**
     * 通话响应状态(NO ANSWER/FAILED/BUSY/ANSWERED/CONGESTION)
     */
    private String disposition;

    /**
     * 混合录音文件地址
     */
    private String voiceAddress;

    /**
     * 主叫一方录音文件地址
     */
    private String voiceAddressIn;

    /**
     * 被叫一方录音文件地址
     */
    private String voiceAddressOut;

    /**
     * 混合录音文件md5值
     */
    private String md5Value;

    /**
     * 主叫录音文件md5值
     */
    private String md5ValueIn;

    /**
     * 被叫录音文件md5值
     */
    private String md5ValueOut;

    /**
     * 分机号
     */
    private String extensionNum;

    /**
     * 用户自定义数据
     */
    private String userData;

    /**
     * 上平台时间(YYYY-MM-DD HH:MM:SS)
     */
    private String callIn;

    /**
     * 应答时刻(YYYY-MM-DD HH:MM:SS)
     */
    private String answer;

    /**
     * 挂机时刻(YYYY-MM-DD HH:MM:SS)
     */
    private String hungUp;

    /**
     * 挂断事件发起方(0主叫 1被叫)
     */
    private String hungUpOriginator;

    /**
     * 响铃时刻(YYYY-MM-DD HH:MM:SS)
     */
    private String startRing;

    /**
     * 匹配到的值守中心座机号码（未匹配为空）
     */
    private String matchPhone;

    /**
     * 通话方向（以归属方视角）：0=未匹配 1=呼出(归属方为主叫) 2=呼入(归属方为被叫)
     */
    private Integer callDirection;
}
