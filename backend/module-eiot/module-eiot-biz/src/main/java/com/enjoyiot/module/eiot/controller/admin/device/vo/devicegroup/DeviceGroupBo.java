package com.enjoyiot.module.eiot.controller.admin.device.vo.devicegroup;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


@Schema(description = "DeviceGroupBo")
@Data
public class DeviceGroupBo {

    private static final long serialVersionUID = -1L;

    @Schema(description = "分组id")
    private String id;

    @Schema(description = "设备组名称")
    @Size(max = 255, message = "设备组名称长度不正确")
    @NotBlank(message = "分组名称不可为空")
    private String name;

    @Schema(description = "分组说明")
    @Size(max = 255, message = "分组说明长度不正确")
    private String remark;

}
