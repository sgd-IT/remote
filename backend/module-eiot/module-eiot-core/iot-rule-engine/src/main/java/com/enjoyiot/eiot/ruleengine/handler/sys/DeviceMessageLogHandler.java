package com.enjoyiot.eiot.ruleengine.handler.sys;


import com.enjoyiot.eiot.IThingModelMessageData;
import com.enjoyiot.eiot.common.thing.ThingModelMessage;
import com.enjoyiot.eiot.ruleengine.handler.DeviceMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author sjg
 */
@Slf4j
@Component
public class DeviceMessageLogHandler implements DeviceMessageHandler {
    @Lazy
    @Autowired
    private IThingModelMessageData thingModelMessageData;

    @Override
    public void handle(ThingModelMessage msg) {
        //设备消息入库
        thingModelMessageData.add(msg);
    }

}
