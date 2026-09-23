package com.enjoyiot.eiot.component.ff.service;

import com.enjoyiot.eiot.common.enums.DeviceState;
import com.enjoyiot.eiot.component.core.ThingComponent;
import com.enjoyiot.eiot.component.core.model.up.DeviceStateChange;
import com.enjoyiot.eiot.component.ff.cilent.FourfaithGbtVertxTcpClient;
import com.enjoyiot.eiot.component.ff.model.FourfaithGbtTcpConfig;
import com.enjoyiot.module.eiot.api.device.DeviceApi;
import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.enjoyiot.module.eiot.api.device.dto.RegisterDevice;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备管理器
 * <p>
 * 从 FourfaithGbtTcpComponent 中提取的设备注册/在线/离线检测逻辑。
 * 统一了之前两套重叠的在线判定机制。
 */
@Slf4j
public class FourfaithGbtDeviceManager {

    private final ThingComponent thingComponent;
    private final DeviceApi deviceApi;

    /** 设备地址(hex) → productKey */
    private final Map<String, String> dnToPk = new ConcurrentHashMap<>();
    /** 部件子设备 deviceName → productKey */
    private final Map<String, String> compDnToPk = new ConcurrentHashMap<>();
    /** 设备地址(hex) → 最后活跃时间（统一在线判定基准） */
    private final Map<String, Long> lastActiveTime = new ConcurrentHashMap<>();
    /** 设备地址(hex) → TCP客户端 */
    private final Map<String, FourfaithGbtVertxTcpClient> clientMap = new ConcurrentHashMap<>();

    /** 备用产品Key（用于未知设备自动注册） */
    private volatile String fallbackProductKey;
    /** 离线超时时间（毫秒） */
    private volatile long offlineTimeoutMs = 600_000L; // 默认10分钟

    public FourfaithGbtDeviceManager(ThingComponent thingComponent, DeviceApi deviceApi) {
        this.thingComponent = thingComponent;
        this.deviceApi = deviceApi;
    }

    /**
     * 更新配置
     */
    public void updateConfig(FourfaithGbtTcpConfig config) {
        this.fallbackProductKey = config.getFallbackProductKey();
    }

    // ==================== 设备注册 ====================

    /**
     * 确保设备已注册
     */
    public String ensureDeviceRegistered(String deviceAddr) {
        if (dnToPk.containsKey(deviceAddr)) {
            return dnToPk.get(deviceAddr);
        }
        DeviceInfo existingDevice = deviceApi.getDeviceByDn(deviceAddr);
        if (existingDevice != null) {
            String pk = existingDevice.getProductKey();
            dnToPk.put(deviceAddr, pk);
            log.info("[四信-GBT26875] 设备已预注册: pk={}, dn={}, tenantId={}", pk, deviceAddr, existingDevice.getTenantId());
            return pk;
        }
        String pk = fallbackProductKey;
        if (pk == null || pk.isEmpty()) {
            log.warn("[四信-GBT26875] 未配置 fallbackProductKey，无法自动注册设备 {}", deviceAddr);
            return null;
        }
        log.info("[四信-GBT26875] 设备 {} 未预注册，自动注册: pk={}", deviceAddr, pk);
        dnToPk.put(deviceAddr, pk);
        RegisterDevice build = RegisterDevice.builder().productKey(pk).deviceName(deviceAddr).build();
        DeviceInfo registered = deviceApi.registerDevice(build);
        if (registered != null) {
            log.info("[四信-GBT26875] 设备自动注册成功: pk={}, dn={}", pk, deviceAddr);
            return pk;
        } else {
            log.error("[四信-GBT26875] 设备自动注册失败: dn={}", deviceAddr);
            dnToPk.remove(deviceAddr);
            return null;
        }
    }

    /**
     * 确保部件子设备已注册
     */
    public String ensureComponentRegistered(String parentAddr, String parentPk,
                                             int hostNo, int circuitNo, int addressNo) {
        String partAddr = FourfaithGbtTcpComponent.generatePartAddress(hostNo, circuitNo, addressNo);
        String cachedPk = compDnToPk.get(partAddr);
        if (cachedPk != null) return cachedPk;

        DeviceInfo parentDevice = deviceApi.getDeviceByDn(parentAddr);
        if (parentDevice == null || parentDevice.getId() == null) {
            log.warn("[四信-GBT26875] 父设备未找到: addr={}", parentAddr);
            return null;
        }
        DeviceInfo existing = deviceApi.getDeviceByParentIdAndDn(parentDevice.getId(), partAddr);
        if (existing != null) {
            String pk = existing.getProductKey();
            compDnToPk.put(partAddr, pk);
            return pk;
        }
        log.warn("[四信-GBT26875] 子设备未预注册: dn={}, parent={}", partAddr, parentAddr);
        return null;
    }

    /**
     * 获取设备的 productKey
     */
    public String getProductKey(String deviceAddr) {
        return dnToPk.get(deviceAddr);
    }

    // ==================== 在线/离线管理 ====================

    /**
     * 更新设备活跃时间（统一入口，替代之前两个独立的 Map）
     */
    public void updateActive(String deviceAddr) {
        lastActiveTime.put(deviceAddr, System.currentTimeMillis());
    }

    /**
     * 设备上线
     */
    public void online(String deviceAddr) {
        updateActive(deviceAddr);
        String pk = dnToPk.get(deviceAddr);
        if (pk != null && !pk.isEmpty()) {
            thingComponent.report(DeviceStateChange.builder()
                    .deviceName(deviceAddr).productKey(pk).state(DeviceState.ONLINE).build());
        }
    }

    /**
     * 绑定 TCP 客户端
     */
    public void bindClient(String deviceAddr, FourfaithGbtVertxTcpClient client) {
        clientMap.put(deviceAddr, client);
        updateActive(deviceAddr);
    }

    /**
     * 获取 TCP 客户端
     */
    public FourfaithGbtVertxTcpClient getClient(String deviceAddr) {
        return clientMap.get(deviceAddr);
    }

    /**
     * 移除客户端（连接断开时调用）
     */
    public void removeClient(FourfaithGbtVertxTcpClient client) {
        clientMap.entrySet().removeIf(e -> e.getValue() == client);
    }

    /**
     * 统一离线检测（每40秒调用一次）
     * <p>
     * 统一判定逻辑：最后活跃时间超过 offlineTimeoutMs 则标记离线。
     * 不再区分 TCP 层和心跳层两套独立机制。
     */
    public void checkOffline() {
        long now = System.currentTimeMillis();

        // 检查 TCP 客户端连接状态
        Set<String> clients = new HashSet<>(clientMap.keySet());
        for (String key : clients) {
            FourfaithGbtVertxTcpClient client = clientMap.get(key);
            if (client != null && !client.isOnline()) {
                client.shutdown();
                clientMap.remove(key);
                lastActiveTime.remove(key);
            }
        }

        // 检查活跃超时（统一判定）
        Set<String> activeKeys = new HashSet<>(lastActiveTime.keySet());
        for (String addr : activeKeys) {
            Long time = lastActiveTime.get(addr);
            if (time != null && now - time > offlineTimeoutMs) {
                lastActiveTime.remove(addr);
                String pk = dnToPk.get(addr);
                if (pk != null) {
                    log.warn("[四信-GBT26875] 设备离线: dn={}, 超时={}ms", addr, offlineTimeoutMs);
                    thingComponent.report(DeviceStateChange.builder()
                            .productKey(pk).deviceName(addr)
                            .state(DeviceState.OFFLINE).time(now).build());
                }
            }
        }
    }

    /**
     * 设置离线超时时间
     */
    public void setOfflineTimeoutMs(long offlineTimeoutMs) {
        this.offlineTimeoutMs = offlineTimeoutMs;
    }
}
