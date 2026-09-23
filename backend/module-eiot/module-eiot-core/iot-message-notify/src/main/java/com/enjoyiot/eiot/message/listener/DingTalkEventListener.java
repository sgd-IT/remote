package com.enjoyiot.eiot.message.listener;


import com.enjoyiot.framework.common.util.json.JsonUtils;
import com.enjoyiot.eiot.message.config.VertxManager;
import com.enjoyiot.eiot.message.event.MessageEvent;
import com.enjoyiot.eiot.message.model.DingTalkConfig;
import com.enjoyiot.eiot.message.model.DingTalkMessage;
import com.enjoyiot.module.eiot.api.alert.dto.Message;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * author: EnjoyIot
 * date: 2023-05-08 15:09
 * description:
 **/
@Slf4j
@Component
public class DingTalkEventListener implements MessageEventListener {
    WebClient client = WebClient.create(VertxManager.INSTANCE.getVertx());

    @Override
    @EventListener(classes = MessageEvent.class, condition = "#event.message.channelCode=='DingTalk'")
    public void doEvent(MessageEvent event) {
        Message message = event.getMessage();
        String channelConfig = message.getChannelConfig();
        DingTalkConfig dingTalkConfig = JsonUtils.parseObject(channelConfig, DingTalkConfig.class);

        DingTalkMessage qyWechatMessage = DingTalkMessage.builder()
                .msgtype("text")
                .text(DingTalkMessage.MessageContent.builder().content(message.getFormatContent()).build())
                .build();

        client.postAbs(dingTalkConfig.getDingTalkWebhook()).sendJson(qyWechatMessage)
                .onSuccess(response -> log.info("Received response with status code" + response.statusCode()))
                .onFailure(err -> log.error("Something went wrong " + err.getMessage()));
    }

}
