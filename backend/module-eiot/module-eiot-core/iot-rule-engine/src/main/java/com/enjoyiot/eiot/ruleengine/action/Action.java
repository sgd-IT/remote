package com.enjoyiot.eiot.ruleengine.action;



import com.enjoyiot.eiot.common.thing.ThingModelMessage;

import java.util.List;

public interface Action<T> {

    String getType();

    List<T> getServices();

    /**
     * 执行动作返回执行动作内容
     */
    List<String> execute(ThingModelMessage msg);
}
