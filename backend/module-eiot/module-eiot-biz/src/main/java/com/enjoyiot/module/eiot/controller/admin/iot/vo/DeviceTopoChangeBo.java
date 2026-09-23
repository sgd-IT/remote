package com.enjoyiot.module.eiot.controller.admin.iot.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 设备拓扑图变化
 *
 * @author sjg
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DeviceTopoChangeBo  {




       private Integer status ;

       private List<DeviceInfo> subList;



    @Data
    public static class DeviceInfo{
        private String deviceName;
        private String productKey;

    }
}
