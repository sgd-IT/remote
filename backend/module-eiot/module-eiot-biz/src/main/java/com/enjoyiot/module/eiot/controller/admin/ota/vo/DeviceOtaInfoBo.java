package com.enjoyiot.module.eiot.controller.admin.ota.vo;





import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Author: Enjoy-iot
 * @Date: 2023/6/17 20:45
 * @Description:
 */
@Data
@Schema(description = "DeviceOtaInfoBo")
public class DeviceOtaInfoBo {

    private Long id;

    private Long packageId;

    private String taskId;

    private String version;

    private String deviceId;

    private String productKey;

    private String deviceName;
}
