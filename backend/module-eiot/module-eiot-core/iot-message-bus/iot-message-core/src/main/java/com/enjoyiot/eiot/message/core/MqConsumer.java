package com.enjoyiot.eiot.message.core;

public interface MqConsumer<T> {

    void consume(String topic, ConsumerHandler<T> handler);

}
