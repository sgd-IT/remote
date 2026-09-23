package com.enjoyiot.module.eiot.controller.admin.rule.vo;


import com.enjoyiot.module.eiot.api.task.dto.RuleAction;
import com.enjoyiot.module.eiot.api.task.dto.TaskInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Size;
import java.util.List;


@Schema(description = "TaskInfoBo")
@Data
public class TaskInfoSaveReqVo {
    private static final long serialVersionUID = -1L;

    @Schema(description = "id")
    private Long id;

    @Schema(description = "任务输出")
    private List<RuleAction> actions;

    @Schema(description = "创建时间")
    private Long createAt;

    @Schema(description = "描述")
    @Size(max = 255, message = "描述长度不正确")
    private String remark;

    @Schema(description = "表达式")
    @Size(max = 255, message = "表达式长度不正确")
    private String expression;

    @Schema(description = "任务名称")
    @Size(max = 255, message = "任务名称长度不正确")
    private String name;

    @Schema(description = "操作备注")
    @Size(max = 255, message = "操作备注长度不正确")
    private String reason;

    @Schema(description = "任务状态")
    @Size(max = 255, message = "任务状态长度不正确")
    private String state;

    @Schema(description = "任务类型")
    @Size(max = 255, message = "任务类型长度不正确")
    private String type;

    @Schema(description = "延时时长秒")
    private Integer seconds;

    @Schema(description = "创建者")
    @Size(max = 255, message = "创建者长度不正确")
    private String uid;

    public void setSeconds(Integer seconds) {
        if (TaskInfo.TYPE_DELAY.equals(getType())) {
            setExpression("" + seconds);
        }
    }
}
