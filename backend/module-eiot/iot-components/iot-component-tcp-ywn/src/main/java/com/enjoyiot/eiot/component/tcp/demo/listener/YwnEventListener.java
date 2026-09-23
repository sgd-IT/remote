package com.enjoyiot.eiot.component.tcp.demo.listener;

import com.enjoyiot.eiot.component.tcp.demo.model.YwnDeviceInfo;
import com.enjoyiot.eiot.component.tcp.demo.model.YwnEvent;
import com.enjoyiot.eiot.component.tcp.demo.model.YwnFrame;

/**
 * 控制器监控TCP协议V27 — 事件监听接口
 *
 * 业务系统实现此接口以接收协议层回调
 */
public interface YwnEventListener {

    /**
     * 设备连接上线
     * @param deviceNumber 设备编号
     */
    default void onDeviceConnected(int deviceNumber) {}

    /**
     * 设备断开连接
     * @param deviceNumber 设备编号
     */
    default void onDeviceDisconnected(int deviceNumber) {}

    /**
     * 收到设备信息上报 (命令0x81)
     * 开机上报一次，每隔24小时上报一次
     * @param deviceInfo 设备信息
     */
    default void onDeviceInfo(YwnDeviceInfo deviceInfo) {}

    /**
     * 收到事件上报 (命令0x80)
     * @param deviceNumber 设备编号
     * @param event 事件数据
     */
    default void onEvent(int deviceNumber, YwnEvent event) {}

    /**
     * 收到心跳 (命令0x82)
     * 周期3分钟
     * @param deviceNumber 设备编号
     */
    default void onHeartbeat(int deviceNumber) {}

    /**
     * 控制器故障 (命令0x86)
     * 2分钟未收到控制器数据触发
     * @param deviceNumber 设备编号
     */
    default void onControllerFault(int deviceNumber) {}

    /**
     * 控制器故障恢复 (命令0x87)
     * @param deviceNumber 设备编号
     */
    default void onControllerFaultRecovery(int deviceNumber) {}

    /**
     * 收到设备对控制命令的应答 (命令0x01)
     * @param deviceNumber 设备编号
     * @param originalFrame 原始控制帧(服务端之前下发的命令)
     */
    default void onControlAck(int deviceNumber, YwnFrame originalFrame) {}

    /**
     * 收到未知帧
     * @param frameData 原始帧数据
     */
    default void onUnknownFrame(byte[] frameData) {}
}
