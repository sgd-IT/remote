package com.enjoyiot.eiot.component.ff.service;

import com.enjoyiot.eiot.component.ff.model.PendingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 待响应请求缓存管理器
 *
 * 负责缓存 remote_control 等异步服务调用，通过业务流水号（serialNumber）
 * 关联下行请求与设备后续上行应答。
 */
@Slf4j
@Component
public class PendingRequestManager {

    private final Map<Integer, PendingRequest> pendingMap = new ConcurrentHashMap<>();

    /** 默认超时时间：30 秒 */
    private static final long TIMEOUT_MS = 30000;

    /**
     * 注册一个待响应请求
     */
    public void put(int serialNumber, PendingRequest request) {
        pendingMap.put(serialNumber, request);
        log.info("[PendingRequest] 注册: SN={}, service={}, invokeId={}",
            serialNumber, request.getServiceName(), request.getInvokeId());
    }

    /**
     * 获取待响应请求（不移除）
     */
    public PendingRequest get(int serialNumber) {
        return pendingMap.get(serialNumber);
    }

    /**
     * 移除并返回待响应请求
     */
    public PendingRequest remove(int serialNumber) {
        PendingRequest req = pendingMap.remove(serialNumber);
        if (req != null) {
            log.info("[PendingRequest] 移除: SN={}, elapsed={}ms",
                serialNumber, System.currentTimeMillis() - req.getSendTime());
        }
        return req;
    }

    /**
     * 超时检查，返回被清理的超时请求列表（由调用方负责上报）
     */
    public List<PendingRequest> timeoutCheck() {
        long now = System.currentTimeMillis();
        List<PendingRequest> timeoutList = new ArrayList<>();
        Iterator<Map.Entry<Integer, PendingRequest>> it = pendingMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, PendingRequest> entry = it.next();
            PendingRequest req = entry.getValue();
            if (now - req.getSendTime() > TIMEOUT_MS) {
                it.remove();
                log.warn("[PendingRequest] 超时: SN={}, service={}",
                    req.getSerialNumber(), req.getServiceName());
                timeoutList.add(req);
            }
        }
        return timeoutList;
    }

    /**
     * 当前待响应请求数量
     */
    public int size() {
        return pendingMap.size();
    }
}
