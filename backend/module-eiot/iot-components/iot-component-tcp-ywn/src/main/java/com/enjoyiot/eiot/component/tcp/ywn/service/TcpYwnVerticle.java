
package com.enjoyiot.eiot.component.tcp.ywn.service;


import com.enjoyiot.eiot.component.tcp.ywn.model.TcpYwnConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * YWN TCP 服务启停管理
 */
@Slf4j
@Component
@Data
public class TcpYwnVerticle extends AbstractVerticle {

    private TcpYwnComponent tcpYwnComponent;
    private NetServer netServer;

    public void startServer(TcpYwnConfig config) {
        NetServerOptions options = new NetServerOptions()
                .setPort(config.getPort())
                .setHost(config.getHost());

        Promise<NetServer> promise = Promise.promise();
        netServer = vertx.createNetServer(options);
        tcpYwnComponent.setTcpServer(netServer, vertx);

        netServer.listen(ar -> {
            if (ar.succeeded()) {
                log.info("[YWN] TCP服务启动成功，监听端口 {}", ar.result().actualPort());
                promise.complete(ar.result());
            } else {
                log.error("[YWN] TCP服务启动失败", ar.cause());
                promise.fail(ar.cause());
            }
        });

        promise.future().onFailure(throwable -> {
            log.error("[YWN] 启动TCP服务器失败: {}", throwable.getMessage());
        });
    }

    public void stopServer() {
        if (netServer != null) {
            netServer.close(rst -> {
                if (rst.succeeded()) {
                    log.info("[YWN] TCP服务器已成功关闭");
                } else {
                    log.error("[YWN] 关闭TCP服务器失败: {}", rst.cause().getMessage());
                }
            });
        } else {
            log.warn("[YWN] TCP服务器尚未启动或已关闭");
        }
    }
}
