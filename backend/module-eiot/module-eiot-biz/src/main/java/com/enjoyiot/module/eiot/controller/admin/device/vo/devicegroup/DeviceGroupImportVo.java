package com.enjoyiot.module.eiot.controller.admin.device.vo.devicegroup;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
@Accessors(chain = false) // 设置 chain = false，避免用户导入有问题
public class DeviceGroupImportVo {

    private static final long serialVersionUID = -1L;

    @ExcelProperty(value = "设备组名称")
    private String name;

    @ExcelProperty(value = "分组说明")
    private String remark;

    /**
     * 分组排序
     */
    @ExcelProperty(value = "分组顺序")

    private Integer groupOrder;
}
