
package com.enjoyiot.eiot.component.tcp;


import com.enjoyiot.eiot.component.tcp.ywn_v2.service.TcpYwnV2Verticle;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Slf4j
@Component
public class TcpYwnV2Starter {

    @Resource
    private TcpYwnV2Verticle tcpYwnV2Verticle;

    @PostConstruct
    public void init() {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(tcpYwnV2Verticle, ar -> {
            if (ar.succeeded()) {
                log.info("start tcp-ywn-v2 component success!");
            } else {
                log.error("start tcp-ywn-v2 component failed", ar.cause());
            }
        });
    }
}