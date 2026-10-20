package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.assist;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 告警辅助判断 - 设备 VO
 *
 * @author sgd
 */
@Schema(description = "管理后台 - 告警辅助判断设备 VO")
@Data
public class AlarmAssistDeviceVO {

    @Schema(description = "设备ID", example = "17")
    private Long deviceId;

    @Schema(description = "设备名称", example = "热成像摄像头")
    private String name;

    @Schema(description = "设备唯一标识", example = "CE0DD7APAGA8B59")
    private String dn;

    @Schema(description = "设备类型：transfer=用传, camera=摄像头, smoke=烟感, temperature=温感, temp_humidity=温湿度, door_magnet=门磁, gas=燃气, hydrant=消火栓, manual=手报", example = "camera")
    private String type;

    @Schema(description = "位置（空间完整路径，无空间回退设备地址快照）", example = "1号楼 / 3层 / 东区 / 东侧走廊北端")
    private String location;

    @Schema(description = "是否在线", example = "true")
    private Boolean online;

    @Schema(description = "近30分钟内是否有未闭环告警（报警中）", example = "false")
    private Boolean alarming;

    @Schema(description = "数据时间（在线时间，缺失回退更新时间）", example = "1791858877663")
    private Long dataTime;

    @Schema(description = "可反控的用传设备ID（非空表示可进设备控制室）", example = "7")
    private Long transferDeviceId;

    @Schema(description = "该设备显式绑定的摄像头")
    private List<AlarmAssistCameraVO> cameras;
}
