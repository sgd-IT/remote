package com.enjoyiot.eiot.temporal.kw.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@TableName("task_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KwTaskLog {

    private Timestamp time;

    private Long taskId;

    private String content;

    private Boolean success;

}
