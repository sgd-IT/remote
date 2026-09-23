package com.enjoyiot.eiot.message.service;

import com.enjoyiot.eiot.message.event.MessageEvent;
import com.enjoyiot.module.eiot.api.alert.dto.Message;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * author: EnjoyIot
 * date: 2023-05-08 16:02
 * description:
 **/
@Service
public class MessageService {
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    public void sendMessage(Message message) {
        System.out.println("发送消息=============》" + message.toString());
        applicationEventPublisher.publishEvent(new MessageEvent(message));
    }

}
