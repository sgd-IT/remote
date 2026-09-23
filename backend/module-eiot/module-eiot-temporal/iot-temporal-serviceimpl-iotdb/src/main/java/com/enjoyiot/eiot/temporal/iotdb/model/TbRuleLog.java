package com.enjoyiot.eiot.temporal.iotdb.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TbRuleLog {

    private Long time;

    private String ruleId;

    private String state1;

    private String content;

    private Boolean success;

}
