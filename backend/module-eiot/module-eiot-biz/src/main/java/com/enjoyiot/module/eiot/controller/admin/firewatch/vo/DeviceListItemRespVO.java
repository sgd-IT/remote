package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 设备列表项 Response VO（用于设备展示页）
 *
 * @author EnjoyIot
 */
@Schema(description = "管理后台 - 设备列表项 Response VO")
@Data
public class DeviceListItemRespVO {

    @Schema(description = "设备ID")
    private Long id;

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "设备唯一标识(DN)")
    private String dn;

    @Schema(description = "设备状态: 0-离线, 1-在线, 3-未激活, 4-故障")
    private Integer state;

    @Schema(description = "地址")
    private String addr;

    @Schema(description = "所属租户ID")
    private Long tenantId;

    @Schema(description = "所属企业名称")
    private String propertyName;

    @Schema(description = "产品Key")
    private String productKey;

    @Schema(description = "序列号")
    private String serialNo;

    @Schema(description = "设备类型：transfer(用传设备), camera(摄像头) 等")
    private String deviceType;

    @Schema(description = "线路类型：0=多线, 1=总线")
    private Integer lineType;
}
