package com.enjoyiot.eiot.component.udp.service;

import com.enjoyiot.eiot.component.udp.dispatcher.UdpPacketDispatcher;
import com.enjoyiot.eiot.component.udp.model.UdpConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;

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

    public void setUdpComponent(UdpComponent udpComponent) {
        this.udpComponent = udpComponent;
    }

    public void startServer(UdpConfig config) {
        // 确保在 Vert.x EventLoop 线程上创建 socket，否则在 Windows 上可能收不到数据
        vertx.runOnContext(v -> {
            socket = vertx.createDatagramSocket();
            socket.listen(config.getPort(), config.getHost(), ar -> {
                if (ar.succeeded()) {
                    log.info("[UDP协议] 服务启动监听 {}:{}", config.getHost(), config.getPort());
                    // 注册业务处理
                    socket.handler(this::handlePacket);
                    // 离线判定交给 DeviceStateCheckTask（按产品 keep_alive_time），UDP 侧不再做超时清理
                } else {
                    log.error("[UDP协议] 服务启动失败", ar.cause());
                }
            });
        });
    }

    public void stopServer() {
        if (socket != null) {
            socket.close();
            log.info("[UDP协议] 服务已停止");
        }
    }

    private void handlePacket(DatagramPacket packet) {
        String senderAddr = packet.sender().host() + ":" + packet.sender().port();
        log.info("[UDP协议] 收到报文 from {}:{} hex={}", packet.sender().host(), packet.sender().port(), Hex.encodeHexString(packet.data().getBytes()));
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
            log.warn("[UDP协议] socket 未初始化，无法发送报文");
            return;
        }
        int colonIdx = senderAddr.lastIndexOf(':');
        if (colonIdx < 0) {
            log.warn("[UDP协议] 地址格式错误: {}", senderAddr);
            return;
        }
        String host = senderAddr.substring(0, colonIdx);
        int port = Integer.parseInt(senderAddr.substring(colonIdx + 1));
        socket.send(Buffer.buffer(data), port, host)
                .onSuccess(v -> log.info("[UDP协议] 发送报文 to {}:{} len={}", host, port, data.length))
                .onFailure(err -> log.error("[UDP协议] 发送报文失败 to {}:{}", host, port, err));
    }
}
