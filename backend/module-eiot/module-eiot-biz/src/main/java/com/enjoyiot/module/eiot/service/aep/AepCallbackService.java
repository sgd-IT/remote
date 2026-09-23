package com.enjoyiot.module.eiot.service.aep;

import com.enjoyiot.module.eiot.controller.admin.aep.dto.AepPushMessage;

/**
 * AEP 平台 HTTP 推送回调处理服务
 */
public interface AepCallbackService {

    /**
     * 异步处理 AEP 推送消息
     * 将 AEP 消息转换为 ThingModelMessage 并发布到消息总线
     *
     * @param message AEP 推送消息
     */
    void processPushMessage(AepPushMessage message);
}
