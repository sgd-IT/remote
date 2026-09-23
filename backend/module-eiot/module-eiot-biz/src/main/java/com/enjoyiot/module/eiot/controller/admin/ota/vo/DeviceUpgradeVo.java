package com.enjoyiot.module.eiot.controller.admin.ota.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Enjoy-iot
 * @Date: 2023/7/18 21:48
 * @Description:
 */
@Data
@Builder
public class DeviceUpgradeVo implements Serializable {
    private String result;
}
