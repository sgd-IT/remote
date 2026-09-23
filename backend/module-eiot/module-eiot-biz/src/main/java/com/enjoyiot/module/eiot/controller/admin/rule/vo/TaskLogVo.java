package com.enjoyiot.module.eiot.controller.admin.rule.vo;





import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


@Schema(description = "TaskLogVo")
@Data

public class TaskLogVo implements Serializable {
    private static final long serialVersionUID = -1L;

    private String id;

    private String taskId;

    private String content;

    private Boolean success;

    private Long logAt;
}
