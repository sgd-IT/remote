package com.enjoyiot.module.eiot.controller.app.cameraalert.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 摄像头报警事件类型
 */
@Getter
@AllArgsConstructor
public enum CameraEventTypeEnum {

    TRIPLE_EVENT(1, "1","高温事件报警"),
    THERMAL_EVENT(2, "1","热成像事件报警"),
    FIRE_SMOKE_EVENT(3, "1","烟火事件报警"),
    AWAY_POST_EVENT(4,"1", "离岗事件报警"),
    CHANNEL_BLOCK_EVENT(5, "1","消防通道占用事件报警");

    private final Integer type;
    private final String level;
    private final String alertName;

    /**
     * 根据 eventType 获取枚举，未匹配返回 null
     */
    public static CameraEventTypeEnum valueOfType(Integer type) {
        if (type == null) {
            return null;
        }
        for (CameraEventTypeEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 根据 eventType 获取报警名称，未匹配返回默认名称
     */
    public static CameraEventTypeEnum getAlertNameByType(Integer type) {
        CameraEventTypeEnum e = valueOfType(type);
        return e;
    }
}
