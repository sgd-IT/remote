package com.enjoyiot.module.eiot.controller.admin.rule.vo;





import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Schema(description = "TaskLogPageReq")
@Data

public class TaskLogPageReq extends PageParam {

    @NotNull(message = "任务id不许为空")


    private Long taskId;


    private Boolean success;

    private Long logAt;
}
