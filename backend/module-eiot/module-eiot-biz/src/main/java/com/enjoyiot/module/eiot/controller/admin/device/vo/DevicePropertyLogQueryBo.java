package com.enjoyiot.module.eiot.controller.admin.device.vo;


import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "DevicePropertiesLogQueryBo")
@Data
public class DevicePropertyLogQueryBo extends PageParam {
    @Schema(description="设备id")
    private Long deviceId;
    @Schema(description="属性名称")
    private String name;
    @Schema(description="开始时间")
    private long start;
    @Schema(description="结束时间")
    private long end;
}
