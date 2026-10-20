package com.enjoyiot.module.eiot.service.alarm;

/**
 * 告警四维语义工具（报警类型 / 报警等级 / 确认档位）
 *
 *   报警类型 alarm_kind：1=火警 2=故障 3=隐患 4=离线 5=误报 6=信息（无序分类）；
 *   报警等级 level：1=一级 2=二级 3=三级（有序，1 最急）；
 *   确认档位 tier：1=Ⅰ档 2=Ⅱ档 3=Ⅲ档 4=Ⅳ档（仅一级事件，有序，1 最急）。
 *
 * @author EnjoyIot
 */
public final class AlarmSemantics {

    /** 报警类型：火警（火灾类信号） */
    public static final int KIND_FIRE = 1;
    /** 报警类型：故障（设备/系统功能异常） */
    public static final int KIND_FAULT = 2;
    /** 报警类型：隐患（影响消防安全的非火情问题） */
    public static final int KIND_RISK = 3;
    /** 报警类型：离线（设备不可达，专值专用） */
    public static final int KIND_OFFLINE = 4;
    /** 报警类型：误报（仅报文级误报通知；人工确认误报走处置状态） */
    public static final int KIND_FALSE_ALARM = 5;
    /** 报警类型：信息（通知类，如设备屏蔽、PRT/CRT 事件） */
    public static final int KIND_INFO = 6;

    /** 确认档位：Ⅰ档（人工直接触发，信号本身即确认） */
    public static final int TIER_I = 1;
    /** 确认档位：Ⅱ档（两个独立探测信号"与"逻辑，互为印证） */
    public static final int TIER_II = 2;
    /** 确认档位：Ⅲ档（视频远程复核，人眼看画面） */
    public static final int TIER_III = 3;
    /** 确认档位：Ⅳ档（无其他手段，只能到场） */
    public static final int TIER_IV = 4;

    private AlarmSemantics() {
    }

    /**
     * 定级层：报警类型 → 报警等级（定稿 1.2 / 3.3 拍板口径）
     * <p>
     * 火警→1（一级）；隐患→2（二级）；故障/离线/误报/信息→3（三级）
     */
    public static String deriveLevel(Integer kind) {
        if (kind == null) {
            return null;
        }
        switch (kind) {
            case KIND_FIRE: return "1";
            case KIND_RISK: return "2";
            default:        return "3";
        }
    }

    /**
     * 一级事件兜底档位
     * <p>
     * 当前无部件类型 / 视频覆盖 / 多信号等判定原料，一级事件按 Ⅳ 档（到场核查，向安全侧）兜底；
     * 非一级不设档位（定稿 1.6：tier 有值 ⇔ level=1）。后续原料到位后只能向更急方向调整（只升不降）
     */
    public static Integer defaultTierForKind(Integer kind) {
        return kind != null && kind == KIND_FIRE ? TIER_IV : null;
    }

    /**
     * 等级 → 出口级别文案（critical/major/warning，与 REST 出口新语义映射一致）
     */
    public static String toAlarmLevelText(String level) {
        if ("1".equals(level)) return "critical";
        if ("2".equals(level)) return "major";
        return "warning";
    }

    /**
     * 接入层归一：告警消息类型字符串 → 报警类型（AlarmMessage.alarmType；兼容人工建单的中文标签）
     * <p>
     * 火灾迹象类（fire/smoke/temperature/manual/composite/火警）→火警；fault/power_fault→故障；
     * risk/supervise→隐患；offline→离线；false_alarm→误报；shield/info→信息；无法识别→null
     */
    public static Integer alarmTypeToKind(String alarmType) {
        if (alarmType == null) {
            return null;
        }
        switch (alarmType) {
            case "fire":
            case "smoke":
            case "temperature":
            case "manual":
            case "composite":
            case "火警":        return KIND_FIRE;
            case "fault":
            case "power_fault":
            case "故障":        return KIND_FAULT;
            case "risk":
            case "supervise":
            case "隐患":        return KIND_RISK;
            case "offline":
            case "离线":        return KIND_OFFLINE;
            case "false_alarm":
            case "误报":        return KIND_FALSE_ALARM;
            case "shield":
            case "info":
            case "信息":        return KIND_INFO;
            default:            return null;
        }
    }

    /**
     * 接入层归一：告警级别字符串 → 报警类型（alarmType 无法识别时的次级依据，保持旧回退口径）
     * <p>
     * critical→火警；major→故障；warning→隐患；info→信息
     */
    public static Integer alarmLevelToKind(String alarmLevel) {
        if (alarmLevel == null) {
            return null;
        }
        switch (alarmLevel.toLowerCase()) {
            case "critical": return KIND_FIRE;
            case "major":    return KIND_FAULT;
            case "warning":  return KIND_RISK;
            case "info":     return KIND_INFO;
            default:         return null;
        }
    }

    /**
     * 接入层归一：主机告警配置 level（eiot_alert_config）→ 报警类型
     * <p>
     * 按库内事实编码：1=火警 2=故障 3=电源故障（归故障类）4=离线 5=其他（PRT/CRT 推定信息）；
     * 无法识别→null（不写新语义）。注意：配置管理页选项标签（3=离线 4=反馈）与库内事实存在偏差，
     * 配置层语义收敛属独立子任务（定稿 C1），此处按库内事实映射
     */
    public static Integer alertConfigLevelToKind(String level) {
        if (level == null) {
            return null;
        }
        switch (level) {
            case "1": return KIND_FIRE;
            case "2": return KIND_FAULT;
            case "3": return KIND_FAULT;
            case "4": return KIND_OFFLINE;
            case "5": return KIND_INFO;
            default:  return null;
        }
    }

    /**
     * 接入层归一：设备告警配置 level（eiot_device_alert_config）→ 报警类型
     * <p>
     * 按配置页语义：1=火警 2=故障 3=其他（推定信息，如液压液位报警）；无法识别→null（不写新语义）
     */
    public static Integer deviceConfigLevelToKind(String level) {
        if (level == null) {
            return null;
        }
        switch (level) {
            case "1": return KIND_FIRE;
            case "2": return KIND_FAULT;
            case "3": return KIND_INFO;
            default:  return null;
        }
    }
}
