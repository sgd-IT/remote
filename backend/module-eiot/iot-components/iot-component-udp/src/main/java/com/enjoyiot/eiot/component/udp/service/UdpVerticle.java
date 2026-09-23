package com.enjoyiot.eiot.component.udp.service;

import com.enjoyiot.eiot.component.udp.dispatcher.UdpPacketDispatcher;
import com.enjoyiot.eiot.component.udp.model.UdpConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.net.SocketAddress;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Udp 启动与停止
 *@createtime 2025/5/22 13:28
 *@version 1.0
 *@author stuil
 */
@Slf4j
@Component
@Data
public class UdpVerticle extends AbstractVerticle {

    private DatagramSocket socket;
    private UdpComponent udpComponent;
    private Map<String, Long> lastSeen;
    private static final long TIMEOUT_MS = 60_000;
    private static final long CLEAN_INTERVAL_MS = 30_000;

    public void setUdpComponent(UdpComponent udpComponent) {
        this.udpComponent = udpComponent;
        this.lastSeen = udpComponent.lastSeen;
    }

    public void startServer(UdpConfig config) {
        socket = vertx.createDatagramSocket();
        socket.listen(config.getPort(), config.getHost(), ar -> {
            if (ar.succeeded()) {
                log.info("UDP server listening on {}:{}", config.getHost(), config.getPort());
                // 注册业务处理
                socket.handler(this::handlePacket);
                // 定期清理超时会话
                vertx.setPeriodic(CLEAN_INTERVAL_MS, id -> cleanOffline());
            } else {
                log.error("UDP server start failed", ar.cause());
            }
        });
    }

    public void stopServer() {
        if (socket != null) {
            socket.close();
            log.info("UDP server stopped");
        }
    }

    private void handlePacket(DatagramPacket packet) {
        SocketAddress sender = packet.sender();
        String senderAddr = sender.host() + ":" + sender.port();
        log.info("[UDP] 收到报文 from {}:{} hex={}", sender.host(), sender.port(), Hex.encodeHexString(packet.data().getBytes()));
        UdpPacketDispatcher.dispatch(vertx, udpComponent, packet.data().getBytes(), senderAddr);
    }

    /**
     * 向指定网络地址发送 UDP 报文
     *
     * @param senderAddr 目标地址，格式为 "host:port"
     * @param data       待发送的字节数组
     */
    public void sendPacket(String senderAddr, byte[] data) {
        if (socket == null) {
            log.warn("[UDP] socket 未初始化，无法发送报文");
            return;
        }
        int colonIdx = senderAddr.lastIndexOf(':');
        if (colonIdx < 0) {
            log.warn("[UDP] 地址格式错误: {}", senderAddr);
            return;
        }
        String host = senderAddr.substring(0, colonIdx);
        int port = Integer.parseInt(senderAddr.substring(colonIdx + 1));
        socket.send(Buffer.buffer(data), port, host)
                .onSuccess(v -> log.info("[UDP] 发送报文 to {}:{} len={}", host, port, data.length))
                .onFailure(err -> log.error("[UDP] 发送报文失败 to {}:{}", host, port, err));
    }

    private void cleanOffline() {
        long now = System.currentTimeMillis();
        lastSeen.forEach((key, ts) -> {
            if (now - ts > TIMEOUT_MS) {
                lastSeen.remove(key);
                log.info("UDP session timed out, offline {}", key);
                udpComponent.handleOffline(key);
            }
        });
    }
}
