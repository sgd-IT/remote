package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * 隐患导入 VO
 *
 * @author EnjoyIot
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = false)
public class FireHazardImportVO {

    @ExcelProperty("隐患等级")
    private String hazardLevel;

    @ExcelProperty("隐患描述")
    private String hazardDesc;

    @ExcelProperty("隐患位置")
    private String location;

    @ExcelProperty("发现人")
    private String foundUserName;

    @ExcelProperty("发现日期")
    private LocalDate foundDate;

    @ExcelProperty("整改期限")
    private LocalDate deadline;

    @ExcelProperty("所属物业名称")
    private String propertyName;

    @ExcelProperty("整改建议")
    private String suggestion;

    @ExcelProperty("备注")
    private String remark;

}
