package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.assist;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 告警辅助判断 Response VO（报警弹窗按 alarmId 查询）
 *
 * @author sgd
 */
@Schema(description = "管理后台 - 告警辅助判断 Response VO")
@Data
public class AlarmAssistRespVO {

    @Schema(description = "告警记录ID", example = "31577")
    private Long alarmId;

    @Schema(description = "告警名称", example = "热成像事件报警")
    private String alertName;

    @Schema(description = "告警等级", example = "1")
    private String alertLevel;

    @Schema(description = "报警设备（告警未关联设备时为 null）")
    private AlarmAssistDeviceVO alarmDevice;

    @Schema(description = "报警设备的安装空间（null = 未配置安装空间）")
    private AlarmAssistSpaceVO space;

    @Schema(description = "同区域设备（同楼栋+楼层+区域，详细位置可不同，排除报警设备自身）")
    private List<AlarmAssistDeviceVO> relatedDevices;

    @Schema(description = "报警设备显式绑定的摄像头（供视频弹层与看视频链接）")
    private List<AlarmAssistCameraVO> ownCameras;
}
