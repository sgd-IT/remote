package com.enjoyiot.eiot.message.config;

import io.vertx.core.Vertx;

public enum VertxManager {
    INSTANCE;
    public Vertx getVertx() {
        return Vertx.vertx();
    }
}
