package com.enjoyiot.eiot.temporal.timescaledb.dm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PgField {
    private String name;
    private String type;
    private int length;
}
