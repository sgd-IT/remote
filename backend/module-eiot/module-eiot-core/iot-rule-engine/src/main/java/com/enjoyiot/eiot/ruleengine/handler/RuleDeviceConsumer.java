package com.enjoyiot.eiot.ruleengine.handler;


import com.enjoyiot.eiot.common.constant.Constants;
import com.enjoyiot.eiot.common.thing.ThingModelMessage;
import com.enjoyiot.eiot.message.core.ConsumerHandler;
import com.enjoyiot.eiot.message.core.MqConsumer;
import com.enjoyiot.framework.common.util.thread.ThreadUtil;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.Order;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
public class RuleDeviceConsumer implements ConsumerHandler<ThingModelMessage>, ApplicationContextAware {

    private final List<DeviceMessageHandler> handlers = new ArrayList<>();
    private ExecutorService messageHandlerPool;

    @SneakyThrows
    public RuleDeviceConsumer(MqConsumer<ThingModelMessage> consumer) {
        consumer.consume(Constants.THING_MODEL_MESSAGE_TOPIC, this);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, DeviceMessageHandler> handlerMap = applicationContext.getBeansOfType(DeviceMessageHandler.class);
        messageHandlerPool = ThreadUtil.newVirtual("messageHandler");
        //this.handlers.addAll(handlerMap.values());
        List<DeviceMessageHandler> handlerList = Lists.newArrayList(handlerMap.values());
        handlerList = handlerList.stream().sorted(Comparator.comparingInt(a -> {
            Order o = a.getClass().getAnnotation(Order.class);
            if (o == null) {
                return Integer.MAX_VALUE;
            }
            return o.value();
        })).collect(Collectors.toList());
        handlers.addAll(handlerList);
    }

    @SneakyThrows
    @Override
//    @TenantIgnore
    public void handler(ThingModelMessage msg) {
        log.info("received thing model message:{}", msg);
        try {
            CompletableFuture<?> future = CompletableFuture.completedFuture("messageHandler");
            for (DeviceMessageHandler handler : this.handlers) {
                future = future.thenRunAsync(() -> {
                    try {
                        extracted(msg, handler);
                    } catch (Throwable e) {
                        log.error("handler message error", e);
                    }
                }, messageHandlerPool);
            }
            future.join(); // 等待所有任务完成
        } catch (Throwable e) {
            log.error("rule device message process error", e);
        }
    }

    private static void extracted(ThingModelMessage msg, DeviceMessageHandler handler) {
        if (!(msg.getData() instanceof Map)) {
            msg.setData(new HashMap<>());
        }
        TenantUtils.executeIgnore(() -> {
                    handler.handle(msg);
                }
        );
    }

}

