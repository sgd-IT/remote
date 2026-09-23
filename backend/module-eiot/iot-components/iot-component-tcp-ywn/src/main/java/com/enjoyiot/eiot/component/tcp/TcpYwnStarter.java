
package com.enjoyiot.eiot.component.tcp;


import com.enjoyiot.eiot.component.tcp.ywn.service.TcpYwnVerticle;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Slf4j
@Component
public class TcpYwnStarter {

    @Resource
    private TcpYwnVerticle tcpYwnVerticle;

    @PostConstruct
    public void init() {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(tcpYwnVerticle, ar -> {
            if (ar.succeeded()) {
                log.info("start tcp-ywn component success!");
            } else {
                log.error("start tcp-ywn component failed", ar.cause());
            }
        });
    }
}