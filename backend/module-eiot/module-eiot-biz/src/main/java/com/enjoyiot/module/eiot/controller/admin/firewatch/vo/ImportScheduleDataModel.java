package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = false)
public class ImportScheduleDataModel {

    @ExcelProperty(value = "排班日期", index = 0)
    private LocalDate scheduleDate;

    @ExcelProperty(value = "班次类型", index = 1)
    private String shiftType;

    @ExcelProperty(value = "班次名称", index = 2)
    private String shiftName;

    @ExcelProperty(value = "值班人姓名", index = 3)
    private String dutyUserName;

    @ExcelProperty(value = "值班人电话", index = 4)
    private String dutyUserPhone;
}