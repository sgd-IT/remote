package com.enjoyiot.module.eiot.controller.admin.rule.vo;


import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.enjoyiot.module.eiot.api.task.dto.RuleAction;
import com.enjoyiot.module.eiot.api.task.dto.TaskInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;


@Schema(description = "TaskInfoVo")
@Data
@ExcelIgnoreUnannotated
public class TaskInfoVo implements Serializable {

    private static final long serialVersionUID = -1L;

    @Schema(description = "主键")
    @ExcelProperty(value = "主键")
    private String id;

    @Schema(description = "任务输出")
    @ExcelProperty(value = "任务输出")
    private List<RuleAction> actions;

    @Schema(description = "创建时间")
    @ExcelProperty(value = "创建时间")
    private Long createAt;

    @Schema(description = "描述")
    @ExcelProperty(value = "描述")
    private String remark;

    @Schema(description = "表达式")
    @ExcelProperty(value = "表达式")
    private String expression;

    @Schema(description = "任务名称")
    @ExcelProperty(value = "任务名称")
    private String name;

    @Schema(description = "操作备注")
    @ExcelProperty(value = "操作备注")
    private String reason;

    @Schema(description = "任务状态")
    @ExcelProperty(value = "任务状态")
    private String state;

    @Schema(description = "任务类型")
    @ExcelProperty(value = "任务类型")
    private String type;

    @Schema(description = "延时时长秒")
    private Integer seconds;

    @Schema(description = "创建者")
    @ExcelProperty(value = "创建者")
    private String uid;

    public void setSeconds(Integer seconds) {
        if (TaskInfo.TYPE_DELAY.equals(getType())) {
            setExpression("" + seconds);
        }
    }

    public Integer getSeconds() {
        if (TaskInfo.TYPE_DELAY.equals(getType())) {
            return Integer.parseInt(getExpression());
        }
        return null;
    }

}
