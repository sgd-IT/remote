package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "远程控制台设备树响应 VO")
public class RemoteConsoleDeviceTreeRespVO {

    @Schema(description = "企业/物业ID")
    private Long id;

    @Schema(description = "企业/物业名称")
    private String name;

    @Schema(description = "风险等级：danger/warning/normal")
    private String riskLevel;

    @Schema(description = "设备数量")
    private Integer deviceCount;

    @Schema(description = "设备列表")
    private List<RemoteConsoleDeviceTreeItemRespVO> devices;
}
