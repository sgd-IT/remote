# 规则引擎报警通知脚本

## 1. 通用 translate 脚本

在规则引擎的"脚本动作"中填入以下内容：

```javascript
this.translate = function(msg, device) {
    var result = {};

    // ========== 设备基础信息 ==========
    result.deviceName = device.name || device.dn || "未知设备";
    result.deviceDn = device.dn || "";
    result.deviceId = device.id || "";
    result.productKey = device.productKey || msg.productKey || "";
    result.deviceModel = device.model || "";
    result.deviceAddr = device.addr || "";
    result.deviceState = device.state === 1 ? "在线" : "离线";

    // ========== 部件子设备信息解析（DN格式：父地址_系统类型_系统地址_部件类型_部件地址）==========
    var isComponentDevice = false;
    var compInfo = { parentAddr: "", sysType: "", sysAddr: "", compType: "", compAddr: "" };
    if (device.dn) {
        var dnParts = device.dn.split("_");
        if (dnParts.length >= 5) {
            isComponentDevice = true;
            compInfo.parentAddr = dnParts[0];
            compInfo.sysType = dnParts[1];
            compInfo.sysAddr = dnParts[2];
            compInfo.compType = dnParts[3];
            compInfo.compAddr = dnParts[4];
        }
    }
    result.isComponentDevice = isComponentDevice ? 1 : 0;
    result.compParentAddr = compInfo.parentAddr;
    result.compSysType = compInfo.sysType;
    result.compSysAddr = compInfo.sysAddr;
    result.compType = compInfo.compType;
    result.compAddr = compInfo.compAddr;

    // ========== 事件/属性参数扁平化 ==========
    if (msg.data && typeof msg.data === 'object') {
        var keys = Object.keys(msg.data);
        for (var i = 0; i < keys.length; i++) {
            var k = keys[i];
            result[k] = msg.data[k];
        }
    }

    // ========== 告警类型自动识别（核心逻辑）==========
    var detectedType = null;

    // 方式1：事件消息直接识别
    if (msg.type === "event") {
        var eventMap = {
            "fire_alarm_event": { level: "紧急", name: "火警", icon: "🔥" },
            "fault_event": { level: "重要", name: "故障", icon: "⚠️" },
            "power_fault_event": { level: "重要", name: "电源故障", icon: "🔌" },
            "offline": { level: "一般", name: "离线", icon: "📡" },
            "online": { level: "提示", name: "上线", icon: "✅" }
        };
        detectedType = eventMap[msg.identifier];
    }

    // 方式2：属性消息根据部件/装置属性识别
    if (!detectedType && msg.type === "property") {
        // 部件子设备属性（comp_ 前缀）
        if (result.comp_fire_alarm === 1) {
            detectedType = { level: "紧急", name: "部件火警", icon: "🔥" };
        } else if (result.comp_fault === 1) {
            detectedType = { level: "重要", name: "部件故障", icon: "⚠️" };
        } else if (result.comp_power_fault === 1) {
            detectedType = { level: "重要", name: "部件电源故障", icon: "🔌" };
        }
        // 父设备装置属性（dev_ 前缀）
        else if (result.dev_fire_alarm === 1) {
            detectedType = { level: "紧急", name: "装置火警", icon: "🔥" };
        } else if (result.dev_fault === 1) {
            detectedType = { level: "重要", name: "装置故障", icon: "⚠️" };
        } else if (result.dev_main_power_fault === 1 || result.dev_backup_power_fault === 1) {
            detectedType = { level: "重要", name: "装置电源故障", icon: "🔌" };
        }
    }

    // 兜底
    if (!detectedType) {
        detectedType = { level: "一般", name: msg.identifier || "告警", icon: "📢" };
    }

    result.alertLevel = detectedType.level;
    result.alertLevelCode = detectedType.level === "紧急" ? "P0" : detectedType.level === "重要" ? "P1" : "P2";
    result.alertIcon = detectedType.icon;
    result.eventTypeName = detectedType.name;

    // ========== 时间格式化 ==========
    if (msg.occurred) {
        var d = new Date(msg.occurred);
        result.eventTime = d.getFullYear() + "-" +
            String(d.getMonth()+1).padStart(2,'0') + "-" +
            String(d.getDate()).padStart(2,'0') + " " +
            String(d.getHours()).padStart(2,'0') + ":" +
            String(d.getMinutes()).padStart(2,'0') + ":" +
            String(d.getSeconds()).padStart(2,'0');
    } else {
        result.eventTime = "";
    }

    // ========== 告警项聚合 ==========
    var alarms = [];
    var details = [];

    // 装置级别
    if (result.dev_fire_alarm === 1) { alarms.push("装置火警"); details.push("装置发生火警"); }
    if (result.dev_fault === 1) { alarms.push("装置故障"); details.push("装置发生故障"); }
    if (result.dev_main_power_fault === 1) { alarms.push("主电源故障"); details.push("主电源故障"); }
    if (result.dev_backup_power_fault === 1) { alarms.push("备用电源故障"); details.push("备用电源故障"); }

    // 部件级别
    if (result.comp_fire_alarm === 1) { alarms.push("部件火警"); details.push("部件发生火警"); }
    if (result.comp_fault === 1) { alarms.push("部件故障"); details.push("部件发生故障"); }
    if (result.comp_power_fault === 1) { alarms.push("部件电源故障"); details.push("部件电源故障"); }

    // 部件描述（如果有）
    if (result.comp_desc) {
        result.componentDesc = result.comp_desc;
        details.push("部件描述：" + result.comp_desc);
    }

    result.alarmItems = alarms.length > 0 ? alarms.join("、") : "正常";
    result.alarmDetail = details.length > 0 ? details.join("；") : "无异常";

    // ========== 通用标题和内容 ==========
    if (isComponentDevice) {
        result.title = detectedType.icon + " " + detectedType.level + "】 " +
            (result.comp_desc || "部件") + " - " + detectedType.name;
        result.content = (result.comp_desc || "部件" + result.compType + "-" + result.compAddr) +
            " 发生" + detectedType.name + "，请及时处理。";
        result.locationInfo = "所属系统：" + result.compSysType + "-" + result.compSysAddr +
            "，部件类型：" + result.compType + "，部件地址：" + result.compAddr;
    } else {
        result.title = detectedType.icon + " " + detectedType.level + "】 " +
            result.deviceName + " - " + detectedType.name;
        result.content = result.deviceName + " 发生" + detectedType.name + "，请及时处理。";
        result.locationInfo = "";
    }

    // 详细描述
    var descLines = [];
    descLines.push("事件类型：" + detectedType.name);
    descLines.push("告警级别：" + detectedType.level);
    if (result.eventTime) descLines.push("发生时间：" + result.eventTime);
    if (result.deviceAddr) descLines.push("设备地址：" + result.deviceAddr);
    if (isComponentDevice && result.locationInfo) descLines.push("部件位置：" + result.locationInfo);
    if (result.comp_desc) descLines.push("部件描述：" + result.comp_desc);
    if (alarms.length > 0) descLines.push("告警项：" + result.alarmItems);
    result.description = descLines.join("\n");

    return result;
};
```

## 2. 通用通知模板

所有规则共用以下模板：

```text
${alertIcon} ${title}
━━━━━━━━━━━━━━━━━━
设备：${deviceName}
编号：${deviceDn}
时间：${eventTime}
状态：${deviceState}
━━━━━━━━━━━━━━━━━━
${content}
━━━━━━━━━━━━━━━━━━
告警详情：
${description}
━━━━━━━━━━━━━━━━━━
```

### 不同渠道适配

**短信模板（精简）：**
```
${alertLevel}告警：${deviceName}(${deviceDn})${eventTime}发生${eventTypeName}，${content}【消防监控】
```

**邮件主题：**
```
【${alertLevelCode}】【${eventTypeName}】${deviceName} - ${eventTime}
```

## 3. 占位符速查表

| 分类 | 占位符 | 示例值 |
|------|--------|--------|
| 设备基础 | `${deviceName}` `${deviceDn}` `${deviceId}` `${productKey}` `${deviceModel}` `${deviceAddr}` `${deviceState}` `${firmVersion}` | 海康国标用传设备 / 000000000000 |
| 事件信息 | `${eventId}` `${eventType}` `${eventIdentifier}` `${eventTypeName}` `${eventCode}` `${eventTime}` `${eventDate}` `${eventTimeOnly}` | 火警 / 2025-04-23 15:30:00 |
| 告警聚合 | `${alertLevel}` `${alertLevelCode}` `${alertIcon}` `${alarmItems}` `${alarmDetail}` `${hasAlarm}` | 紧急 / P0 / 🔥 |
| 通用内容 | `${title}` `${content}` `${description}` `${paramsList}` | 自动生成的标题和内容 |
| 部件专用 | `${isComponentDevice}` `${compParentAddr}` `${compSysType}` `${compSysAddr}` `${compType}` `${compAddr}` `${comp_desc}` `${componentDesc}` `${locationInfo}` | 1 / 000000000000 / 1F-01烟感探测器 |
| 原始参数 | `${dev_fire_alarm}` `${dev_fault}` `${dev_main_power_fault}` `${dev_backup_power_fault}` `${comp_fire_alarm}` `${comp_fault}` `${comp_power_fault}` 及 `${param_xxx}` | 1 / 0 |

## 4. 规则配置对照表

| 序号 | 规则名称 | 监听类型 | 监听标识 | 过滤条件 | 触发场景 |
|------|---------|---------|---------|---------|---------|
| 1 | 用传装置离线事件 | `event` | `offline` | — | 父设备离线 |
| 2 | 用传电源故障事件 | `event` | `power_fault_event` | — | Type 21 装置电源故障 |
| 3 | 用传设备故障事件 | `event` | `fault_event` | — | Type 1/21 装置/系统故障 |
| 4 | 用传设备火警事件 | `event` | `fire_alarm_event` | — | Type 1/2/21 火警 |
| 5 | 子设备部件火警点位 | `property` | `report` | `comp_fire_alarm == 1` | 部件子设备火警属性 |
| 6 | 子设备部件故障点位 | `property` | `report` | `comp_fault == 1` | 部件子设备故障属性 |
| 7 | 子设备部件电源故障 | `property` | `report` | `comp_power_fault == 1` | 部件子设备电源故障属性 |

## 5. 使用说明

1. 在规则引擎的"脚本动作"中粘贴上述 `translate` 函数
2. 在规则引擎的"消息模板"中粘贴通用模板
3. 所有 7 条规则**共用同一个脚本和模板**
4. 脚本会自动识别是事件触发还是属性触发，并生成对应的标题和内容
