package com.enjoyiot.eiot.ruleengine.handler.sys;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import com.enjoyiot.eiot.common.constant.Constants;
import com.enjoyiot.eiot.common.enums.DeviceState;
import com.enjoyiot.eiot.common.thing.ThingModelMessage;
import com.enjoyiot.eiot.message.core.MqProducer;
import com.enjoyiot.eiot.ruleengine.handler.DeviceMessageHandler;
import com.enjoyiot.framework.common.util.collection.CollectionUtils;
import com.enjoyiot.module.eiot.api.device.DeviceApi;
import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.enjoyiot.module.eiot.api.product.ProductApi;
import com.enjoyiot.module.eiot.api.product.dto.Product;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 设备状态检查
 */
@Slf4j
@Component
public class DeviceStateCheckHandler implements DeviceMessageHandler {

    @Autowired
    private DeviceApi deviceApi;

    @Resource
    private MqProducer<ThingModelMessage> producer;

    @Override
    public void handle(ThingModelMessage msg) {
        // 过滤下行消息
        if (Boolean.TRUE.equals(msg.getToClient())) {
            return;
        }
        String type = msg.getType();

        DeviceInfo device = deviceApi.getDeviceInfoFromCache(msg.getDeviceId());
        if (device == null) {
            return;
        }

        DeviceState state = DeviceState.ONLINE;
        String identifier = msg.getIdentifier();

        // 当
        if (ThingModelMessage.TYPE_STATE.equals(type)) {
            state = StringUtils.equals(identifier, "online") ? DeviceState.ONLINE : DeviceState.OFFLINE;
        }
        if (state != DeviceState.OFFLINE) {
            // 上行消息,不是离线状态,都更新最新消息
            updateDeviceLastTime(device, msg);
        }

        // 状态变化检查：
        // 对于显式 state 类型消息（设备上线/下线），始终更新 DB，
        // 避免缓存与 DB 不一致导致状态丢失。
        // 对于其他类型消息（property/event 等），仅在状态实际变化时更新。
        boolean isExplicitStateMsg = ThingModelMessage.TYPE_STATE.equals(type);
        if (!isExplicitStateMsg) {
            // 非显式状态消息，仅在状态实际变化时更新
            if ((device.isOnline() && state == DeviceState.ONLINE) ||
                    (!device.isOnline() && state == DeviceState.OFFLINE)) {
                return;
            }
        }
        log.info("rule engine state change [{} -> {}] msgId: {}, type={}, explicit={}",
                device.isOnline(), state.isOnline(), msg.getMid(), type, isExplicitStateMsg);

        Long time = msg.getTime();
        if (time == null) {
            time = System.currentTimeMillis();
        }
        if (!ThingModelMessage.TYPE_STATE.equals(type)) {
            // 如果非状态改变的消息, 为了避免覆盖原消息日志(时序数据库按时间存,相同时间只存一条),所以-1
            time = time - 1;
        }

        Boolean b = deviceApi.updateDeviceState(device.getId(), state.isOnline());
        if(b && !ThingModelMessage.TYPE_STATE.equals(type)){
            sendDeviceStateChangeMessage(device, state.isOnline() ,time);
        }
        updateSubDeviceState(device, state, b, type, time);

    }

    private void updateSubDeviceState(DeviceInfo device, DeviceState state, Boolean b, String type, Long time) {
        if( DeviceInfo.NODE_TYPE_GATEWAY == device.getNodeType()){
            List<DeviceInfo> subList = deviceApi.getSubDevicesByProductKeAndDeviceName(device.getProductKey(), device.getDn());
            if(CollectionUtil.isNotEmpty(subList)){
                for (DeviceInfo subDevice : subList) {
                    if (subDevice != null && subDevice.getTransparent()){
                        deviceApi.updateDeviceState(subDevice.getId(), state.isOnline());
                        if(b && !ThingModelMessage.TYPE_STATE.equals(type)){
                            sendDeviceStateChangeMessage(subDevice, state.isOnline() , time);
                        }
                    }
                }
            }
        }
    }

    private void updateDeviceLastTime(DeviceInfo device, ThingModelMessage msg) {
        Long msgTimeMilli = msg.getTime();
        deviceApi.updateDeviceLastTimeCache(device.getId(), msgTimeMilli);

    }

    private void sendDeviceStateChangeMessage(DeviceInfo device, Boolean online, Long time) {
        // TODO: 提却到某个公共类中
        String stateId =   (online) ? ThingModelMessage.ID_ONLINE : ThingModelMessage.ID_OFFLINE;
        ThingModelMessage msg = ThingModelMessage.builder()
                .id(IdUtil.fastSimpleUUID())
                .time(time)
                .dn(device.getDn())
                .productKey(device.getProductKey())
                .occurred(time)
                .deviceId(device.getId())
                .identifier(stateId)
                .type(ThingModelMessage.TYPE_STATE)
                .build();
        producer.publish(Constants.THING_MODEL_MESSAGE_TOPIC, msg);

    }
}
