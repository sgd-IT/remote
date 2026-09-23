package com.enjoyiot.module.eiot.api.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskLog {

    private String id;

    private Long taskId;

    private String content;

    private Boolean success;

    private Long logAt;
}
