package com.enjoyiot.eiot.ruleengine.handler;


import com.enjoyiot.eiot.common.thing.ThingModelMessage;

public interface  DeviceMessageHandler {

    void handle(ThingModelMessage message);

}
