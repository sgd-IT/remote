
package com.enjoyiot.eiot.component.gbttcp;


import com.enjoyiot.eiot.component.gbttcp.service.GbtTcpVerticle;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Slf4j
// @Component  // 已停用：由 iot-component-tcp-remote-monitor 替代
public class GbtTcpStarter {

    @Resource
    private GbtTcpVerticle gbtTcpVerticle;

    @PostConstruct
    public void init() {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(gbtTcpVerticle, ar -> {
            if (ar.succeeded()) {
                log.info("start gbt-tcp component success!");
            } else {
                log.error("start gbt-tcp component failed", ar.cause());
            }
        });
    }
}