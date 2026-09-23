package com.enjoyiot.eiot.component.tcp.ywn_v2.cilent;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;

import java.time.Duration;

@Slf4j
public class VertxTcpYwnV2Client {
    @Getter
    private String id;
    public NetSocket socket;
    @Setter
    private long keepAliveTimeoutMs = Duration.ofSeconds(30).toMillis();
    private volatile long lastKeepAliveTime = System.currentTimeMillis();

    @Setter
    private RecordParser parser;

    /** 断连回调（由外部设置，用于清理 clientMap 等） */
    private volatile Runnable onCloseCallback;

    public VertxTcpYwnV2Client(String id) {
        this.id = id;
    }

    /**
     * 设置断连回调，当 socket 关闭时自动调用
     */
    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    public void keepAlive() {
        lastKeepAliveTime = System.currentTimeMillis();
    }

    public boolean isOnline() {
        return System.currentTimeMillis() - lastKeepAliveTime < keepAliveTimeoutMs;
    }

    public void setSocket(NetSocket socket) {
        synchronized (this) {
            if (this.socket != null && this.socket != socket) {
                this.socket.close();
            }

            this.socket = socket
                    .closeHandler(v -> shutdown())
                    .handler(buffer -> {
                        log.info("receive data: hex={}", Hex.encodeHexString(buffer.getBytes()));
                        if (log.isDebugEnabled()) {
                            log.warn("handle tcp client[{}] payload:[{}]",
                                    socket.remoteAddress(),
                                    Hex.encodeHexString(buffer.getBytes()));
                        }
                        keepAlive();
                        parser.handle(buffer);
                        if (this.socket != socket) {
                            log.warn("tcp client [{}] memory leak ", socket.remoteAddress());
                            socket.close();
                        }
                    });
        }
    }

    public void shutdown() {
        log.warn("[YWN] tcp client [{}] disconnect", getId());
        synchronized (this) {
            if (null != socket) {
                execute(socket::close);
                this.socket = null;
            }
        }
        // 触发断连回调（清理 clientMap 等）
        Runnable callback = this.onCloseCallback;
        if (callback != null) {
            try {
                callback.run();
            } catch (Exception e) {
                log.warn("[YWN] 断连回调执行异常", e);
            }
        }
    }

    public void sendMessage(Buffer buffer) {
        log.info("write data: hex={}", Hex.encodeHexString(buffer.getBytes()));
        if (socket == null) {
            log.warn("tcp client [{}] socket is null, drop message", getId());
            return;
        }
        socket.write(buffer, r -> {
            keepAlive();
            if (r.succeeded()) {
                log.info("client msg send success: hex={}", Hex.encodeHexString(buffer.getBytes()));
            } else {
                log.error("client msg send failed", r.cause());
            }
        });
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close tcp client error", e);
        }
    }

}