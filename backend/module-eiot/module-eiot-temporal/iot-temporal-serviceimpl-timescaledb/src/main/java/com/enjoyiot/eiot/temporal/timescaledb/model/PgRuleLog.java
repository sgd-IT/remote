package com.enjoyiot.eiot.temporal.timescaledb.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@TableName("rule_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PgRuleLog {

    private Timestamp time;

    private Long ruleId;

    private String state1;

    private String content;

    private Boolean success;

}
