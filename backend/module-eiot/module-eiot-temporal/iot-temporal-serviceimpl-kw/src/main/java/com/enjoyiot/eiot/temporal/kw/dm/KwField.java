package com.enjoyiot.eiot.temporal.kw.dm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KwField {
    private String name;
    private String type;
    private int length;
}
