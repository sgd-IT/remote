package com.enjoyiot.module.eiot.controller.admin.ota.vo;


import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Author: Enjoy-iot
 * @Date: 2023/6/17 20:45
 * @Description:
 */
@Data
@Schema(description = "DeviceOtaDetailPageReq")
public class DeviceOtaDetailPageReq extends PageParam {

    private Long otaInfoId;

    private String version;

    private String deviceId;

    private String productKey;

    private String deviceName;
}
