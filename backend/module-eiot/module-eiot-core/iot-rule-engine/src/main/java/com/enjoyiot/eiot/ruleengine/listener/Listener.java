package com.enjoyiot.eiot.ruleengine.listener;


import com.enjoyiot.eiot.common.thing.ThingModelMessage;

import java.util.List;

public interface Listener<T> {

    String getType();

    List<T> getConditions();

    boolean execute(ThingModelMessage msg);
}
