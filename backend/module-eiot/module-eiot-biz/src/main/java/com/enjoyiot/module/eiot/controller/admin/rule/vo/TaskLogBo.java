package com.enjoyiot.module.eiot.controller.admin.rule.vo;





import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "TaskLogBo")
@Data

public class TaskLogBo {

    private String id;

    private String taskId;

    private String content;

    private Boolean success;

    private Long logAt;
}
