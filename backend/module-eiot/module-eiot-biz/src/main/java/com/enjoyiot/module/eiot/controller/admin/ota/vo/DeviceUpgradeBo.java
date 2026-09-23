package com.enjoyiot.module.eiot.controller.admin.ota.vo;



import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @Author: Enjoy-iot
 * @Date: 2023/6/16 21:13
 * @Description:
 */
@Schema(description = "DeviceUpgradeBo")
@Data
public class DeviceUpgradeBo {
    private static final long serialVersionUID = -1L;
    private List<Long> deviceIds;
    private Long otaId;
}
