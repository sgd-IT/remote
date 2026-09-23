package com.enjoyiot.module.eiot.controller.admin.device.vo;


import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data

//
public class DeviceLogPageReqVo extends PageParam {

    @Schema(description="设备id")
    private Long deviceId;

    @Schema(description="类型")
    private String type;

    @Schema(description="属性名")
    private String identifier;
}
