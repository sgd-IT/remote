package com.enjoyiot.eiot.message.core;

public interface MqProducer<T> {

    void publish(String topic, T msg);

}
