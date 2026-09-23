package com.enjoyiot.module.eiot.api.task.dto;


import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Size;

@Schema(description = "TaskInfoPageReq")
@Data
public class TaskInfoPageReq extends PageParam {
    private static final long serialVersionUID = -1L;
    
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


}
