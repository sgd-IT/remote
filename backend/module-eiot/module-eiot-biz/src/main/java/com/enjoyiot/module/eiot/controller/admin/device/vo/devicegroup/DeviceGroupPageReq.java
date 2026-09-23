package com.enjoyiot.module.eiot.controller.admin.device.vo.devicegroup;




import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Size;


@Data
public class DeviceGroupPageReq extends PageParam {

    private static final long serialVersionUID = -1L;

    @Schema(description = "分组id")
    private String id;

    @Schema(description = "设备组名称")
    @Size(max = 255, message = "设备组名称长度不正确")
    private String name;

}
