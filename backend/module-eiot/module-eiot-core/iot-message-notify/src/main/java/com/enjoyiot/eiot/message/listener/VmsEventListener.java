package com.enjoyiot.eiot.message.listener;


import com.enjoyiot.eiot.message.config.VertxManager;
import com.enjoyiot.eiot.message.event.MessageEvent;
import com.enjoyiot.framework.common.util.json.JsonUtils;
import com.enjoyiot.module.eiot.api.alert.ChannelVmsStrategy;
import com.enjoyiot.module.eiot.api.alert.dto.Message;
import com.enjoyiot.module.eiot.api.alert.dto.VmsConfig;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * author: EnjoyIot
 * date: 2023-05-08 15:09
 * description:
 **/
@Slf4j
@Component
public class VmsEventListener implements MessageEventListener {
    @Resource
    private ChannelVmsStrategy channelVmsStrategy;

    @Override
    @EventListener(classes = MessageEvent.class, condition = "#event.message.channelCode=='VMS'")
    public void doEvent(MessageEvent event) {
        Message message = event.getMessage();
        String channelConfig = message.getChannelConfig();
        VmsConfig vmsConfig = JsonUtils.parseObject(channelConfig, VmsConfig.class);
        channelVmsStrategy.callByTts(message.getParam(), message.getTemplateCode(), vmsConfig);
    }

}
