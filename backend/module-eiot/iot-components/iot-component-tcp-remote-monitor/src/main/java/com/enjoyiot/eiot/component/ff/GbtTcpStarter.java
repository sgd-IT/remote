
package com.enjoyiot.eiot.component.ff;


import com.enjoyiot.eiot.component.ff.service.GbtTcpVerticle;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Slf4j
@Component("GbtTcpStarter1")
public class GbtTcpStarter {

    @Resource
    private GbtTcpVerticle gbtTcpVerticle;

    @PostConstruct
    public void init() {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(gbtTcpVerticle, ar -> {
            if (ar.succeeded()) {
                log.info("start gbt-remote-monitor component success!");
            } else {
                log.error("start gbt-remote-monitor component failed", ar.cause());
            }
        });
    }
}