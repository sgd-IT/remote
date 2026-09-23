
package com.enjoyiot.eiot.component.ff;


import com.enjoyiot.eiot.component.ff.service.FourfaithGbtTcpVerticle;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Slf4j
@Component
public class FourfaithGbtTcpStarter {

    @Resource
    private FourfaithGbtTcpVerticle fourfaithGbtTcpVerticle;

    @PostConstruct
    public void init() {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(fourfaithGbtTcpVerticle, ar -> {
            if (ar.succeeded()) {
                log.info("start gbt-ff-tcp component success!");
            } else {
                log.error("start gbt-ff-tcp component failed", ar.cause());
            }
        });
    }
}