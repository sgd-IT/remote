package com.enjoyiot.eiot.common.thing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author sjg
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThingService<T> {

    private String mid;

    private String productKey;

    private String dn;

    private String type;

    private String identifier;

    private T params;

}
