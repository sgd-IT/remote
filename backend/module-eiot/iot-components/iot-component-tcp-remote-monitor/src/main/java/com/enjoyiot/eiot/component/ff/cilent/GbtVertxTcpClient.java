package com.enjoyiot.eiot.component.ff.cilent;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;

import java.time.Duration;

@Slf4j
public class GbtVertxTcpClient {
    @Getter
    private String id;
    public NetSocket socket;
    @Setter
    private long keepAliveTimeoutMs = Duration.ofSeconds(900).toMillis();
    private volatile long lastKeepAliveTime = System.currentTimeMillis();

    @Setter
    private RecordParser parser;

    public GbtVertxTcpClient(String id) {
        this.id = id;
    }

    public void keepAlive() {
        lastKeepAliveTime = System.currentTimeMillis();
    }

    public boolean isOnline() {
        return System.currentTimeMillis() - lastKeepAliveTime < keepAliveTimeoutMs;
    }

    /**
     * 绑定连接
     * @param socket
     */
    public void setSocket(NetSocket socket) {
        synchronized (this) {
            if (this.socket != null && this.socket != socket) {
                this.socket.close();
            }

            this.socket = socket
                    //如果连接关闭，则关闭当前连接
                    .closeHandler(v -> shutdown())
                    //处理接收到的数据
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
        log.warn("tcp{}关闭连接", getId());
        synchronized (this) {
            if (null != socket) {
                execute(socket::close);
                this.socket = null;
            }
        }
    }

    public void sendMessage(Buffer buffer) {
        log.info("write data: hex={}", Hex.encodeHexString(buffer.getBytes()));
        if (socket == null) {
            log.warn("tcp{}连接为空，无法发送数据", getId());
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
