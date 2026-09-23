package com.enjoyiot.eiot.temporal.rdb.dm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RdbField {
    private String name;
    private String type;
    private int length;

    public RdbField(String name, String type) {
        this.name = name;
        this.type = type;
        this.length = -1;
    }
}
