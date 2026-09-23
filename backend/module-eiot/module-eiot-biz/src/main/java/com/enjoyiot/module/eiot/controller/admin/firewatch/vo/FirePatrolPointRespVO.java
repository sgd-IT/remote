package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 巡检点位 Response VO")
@Data
public class FirePatrolPointRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "关联任务ID")
    private Long taskId;

    @Schema(description = "关联设备ID")
    private Long deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "巡检位置")
    private String location;

    @Schema(description = "楼层")
    private String floor;

    @Schema(description = "区域")
    private String area;

    @Schema(description = "巡检项目(JSON数组)")
    private String checkItems;

    @Schema(description = "巡检结果")
    private String result;

    @Schema(description = "结果名称")
    private String resultName;

    @Schema(description = "巡检状态")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "巡检时间")
    private LocalDateTime checkTime;

    @Schema(description = "巡检人ID")
    private Long checkerId;

    @Schema(description = "巡检人姓名")
    private String checkerName;

    @Schema(description = "问题描述")
    private String problemDesc;

    @Schema(description = "整改建议")
    private String suggestion;

    @Schema(description = "现场照片(JSON数组)")
    private String photos;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
