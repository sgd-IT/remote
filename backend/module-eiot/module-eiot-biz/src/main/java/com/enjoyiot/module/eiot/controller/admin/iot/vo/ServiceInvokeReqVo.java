package com.enjoyiot.module.eiot.controller.admin.iot.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * @author: regan
 * @description:
 * @date:created in 2023/6/17 12:17
 * @modificed by:
 */
@Schema(description = "ServiceInvokeReqVo")
@Data
public class ServiceInvokeReqVo {
     @Schema(description = "设备id",required = true)
     @NotNull(message = "设备id不许为空")
     private Long deviceId;

     @Schema(description = "服务",required = true)
    @NotBlank(message = "服务不许为空")
    private String service;
     @Schema(description = "参数")
    private Map<String, Object> args;

}
