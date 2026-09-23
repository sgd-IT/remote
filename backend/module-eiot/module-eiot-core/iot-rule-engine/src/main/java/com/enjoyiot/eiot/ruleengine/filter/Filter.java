package com.enjoyiot.eiot.ruleengine.filter;


import com.enjoyiot.eiot.common.thing.ThingModelMessage;

import java.util.List;

/**
 * 场景过滤器
 */
public interface Filter<T> {

    String getType();

    List<T> getConditions();

    void init();

    boolean execute(ThingModelMessage msg);
}
