package com.enjoyiot.module.eiot.controller.admin.device.vo.devicegroup;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


@Schema(description = "DeviceAddGroupBo")
@Data
public class DeviceAddGroupBo {

    private static final long serialVersionUID = -1L;

    @Schema(description = "组id")
    private Long groupId;
    @Schema(description = "设备列表")
    private  List<Long> deviceIds;

}
