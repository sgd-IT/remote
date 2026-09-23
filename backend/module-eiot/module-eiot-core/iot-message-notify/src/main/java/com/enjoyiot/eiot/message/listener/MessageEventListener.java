package com.enjoyiot.eiot.message.listener;


import com.enjoyiot.eiot.message.event.MessageEvent;

/**
 * author: EnjoyIot
 * date: 2023-05-08 15:08
 * description:
 **/
public interface MessageEventListener {

    void doEvent(MessageEvent event);

}
