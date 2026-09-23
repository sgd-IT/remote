package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * 巡检任务导入 VO
 *
 * @author EnjoyIot
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = false)
public class FirePatrolTaskImportVO {

    @ExcelProperty("任务名称")
    private String taskName;

    @ExcelProperty("任务类型")
    private String taskType;

    @ExcelProperty("计划日期")
    private LocalDate planDate;

    @ExcelProperty("所属物业名称")
    private String propertyName;

    @ExcelProperty("巡检人")
    private String patrolUserName;

    @ExcelProperty("联系电话")
    private String patrolUserPhone;

    @ExcelProperty("备注")
    private String remark;

}
