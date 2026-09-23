package com.enjoyiot.module.eiot.dal.dataobject.firewatch;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 巡检点位记录 DO
 *
 * @author EnjoyIot
 */
@TableName("fire_patrol_point")
@KeySequence("fire_patrol_point_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirePatrolPointDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 关联任务ID
     */
    private Long taskId;

    /**
     * 关联设备ID
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
     * 设备类型
     */
    private String deviceType;

    /**
     * 巡检位置
     */
    private String location;

    /**
     * 楼层
     */
    private String floor;

    /**
     * 区域
     */
    private String area;

    /**
     * 巡检项目(JSON数组)
     */
    private String checkItems;

    /**
     * 巡检结果: normal-正常, abnormal-异常
     */
    private String result;

    /**
     * 巡检状态: pending-待检, completed-已检
     */
    private String status;

    /**
     * 巡检时间
     */
    private LocalDateTime checkTime;

    /**
     * 巡检人ID
     */
    private Long checkerId;

    /**
     * 巡检人姓名
     */
    private String checkerName;

    /**
     * 问题描述
     */
    private String problemDesc;

    /**
     * 整改建议
     */
    private String suggestion;

    /**
     * 现场照片(JSON数组)
     */
    private String photos;

    /**
     * 备注
     */
    private String remark;

}
