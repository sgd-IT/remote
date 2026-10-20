package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "智能空开待执行命令响应 VO")
public class ValveCommandRespVO {

    @Schema(description = "操作类型：open=开闸，close=合闸")
    private String action;

    @Schema(description = "设备 IP")
    private String ip;

    @Schema(description = "产品 ID")
    private String productID;

    @Schema(description = "设备 ID")
    private String deviceID;

    @Schema(description = "操作值：1=开闸，0=合闸")
    private Integer actionValue;

    @Schema(description = "命令创建时间")
    private LocalDateTime createdAt;
}
