package com.enjoyiot.eiot.message.core;

public interface ConsumerHandler<T> {

    void handler(T msg);

}
