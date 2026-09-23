# 四信规则引擎报警通知脚本

> 适用协议：四信 GB/T 26875.3-2011 + 私有扩展（Type 9 PRT / Type 10 CRT）
> 物模型版本：fourfaith_parent_thing_model.json / fourfaith_component_thing_model.json
> 报警分类对齐：前端报警配置 config.vue（1=火警, 2=设备故障, 3=电源故障, 4=装置离线, 5=其他）

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
    // 报警分类映射（对齐前端 config.vue）
    // 1=火警, 2=设备故障, 3=电源故障, 4=装置离线, 5=其他
    var detectedType = null;

    // 方式1：事件消息直接识别
    if (msg.type === "event") {
        var eventCategoryMap = {
            "fire_alarm_event": { category: 1, name: "火警", icon: "🔥" },
            "fault_event": { category: 2, name: "设备故障", icon: "⚠️" },
            "power_fault_event": { category: 3, name: "电源故障", icon: "🔌" },
            "prt_alarm_event": { category: 1, name: "PRT打印报警", icon: "🖨️" },
            "crt_alarm_event": { category: 1, name: "CRT图形报警", icon: "🖥️" },
            "offline": { category: 4, name: "装置离线", icon: "📡" },
            "online": { category: 5, name: "装置上线", icon: "✅" }
        };
        detectedType = eventCategoryMap[msg.identifier];

        // CRT alarm_type 语义映射（覆盖默认分类）
        if (msg.identifier === "crt_alarm_event" && result.alarm_type !== undefined) {
            var crtAlarmType = parseInt(result.alarm_type);
            if (crtAlarmType === 1) {
                detectedType = { category: 1, name: "CRT火警", icon: "🔥" };
            } else if (crtAlarmType === 2) {
                detectedType = { category: 2, name: "CRT故障", icon: "⚠️" };
            }
        }
    }

    // 方式2：属性消息根据部件/装置属性识别
    if (!detectedType && msg.type === "property") {
        // 部件子设备属性（comp_ 前缀）
        if (result.comp_fire_alarm === 1) {
            detectedType = { category: 1, name: "部件火警", icon: "🔥" };
        } else if (result.comp_fault === 1) {
            detectedType = { category: 2, name: "部件故障", icon: "⚠️" };
        } else if (result.comp_power_fault === 1) {
            detectedType = { category: 3, name: "部件电源故障", icon: "🔌" };
        }
        // 父设备装置属性（dev_ 前缀）
        else if (result.dev_fire_alarm === 1) {
            detectedType = { category: 1, name: "装置火警", icon: "🔥" };
        } else if (result.dev_fault === 1) {
            detectedType = { category: 2, name: "装置故障", icon: "⚠️" };
        } else if (result.dev_main_power_fault === 1 || result.dev_backup_power_fault === 1) {
            detectedType = { category: 3, name: "装置电源故障", icon: "🔌" };
        }
    }

    // 兜底
    if (!detectedType) {
        detectedType = { category: 5, name: msg.identifier || "告警", icon: "📢" };
    }

    // 报警分类字段（对齐前端）
    var categoryNames = { 1: "火警", 2: "设备故障", 3: "电源故障", 4: "装置离线", 5: "其他" };
    result.alertCategory = detectedType.category;
    result.alertCategoryName = categoryNames[detectedType.category] || "其他";
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
    } else if (result.event_time) {
        result.eventTime = result.event_time;
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
        result.title = detectedType.icon + " " + result.alertCategoryName + "】 " +
            (result.comp_desc || "部件") + " - " + detectedType.name;
        result.content = (result.comp_desc || "部件" + result.compType + "-" + result.compAddr) +
            " 发生" + detectedType.name + "，请及时处理。";
        result.locationInfo = "所属系统：" + result.compSysType + "-" + result.compSysAddr +
            "，部件类型：" + result.compType + "，部件地址：" + result.compAddr;
    } else {
        result.title = detectedType.icon + " " + result.alertCategoryName + "】 " +
            result.deviceName + " - " + detectedType.name;
        result.content = result.deviceName + " 发生" + detectedType.name + "，请及时处理。";
        result.locationInfo = "";
    }

    // ========== 详细描述（增强：事件参数展开）==========
    var descLines = [];
    descLines.push("事件类型：" + detectedType.name);
    descLines.push("报警分类：" + result.alertCategoryName);
    if (result.eventTime) descLines.push("发生时间：" + result.eventTime);
    if (result.deviceAddr) descLines.push("设备地址：" + result.deviceAddr);
    if (isComponentDevice && result.locationInfo) descLines.push("部件位置：" + result.locationInfo);
    if (result.comp_desc) descLines.push("部件描述：" + result.comp_desc);

    // 火警事件参数展开
    if (msg.identifier === "fire_alarm_event") {
        if (result.event_level !== undefined) {
            var levelMap = { "1": "系统级", "2": "部件级", "3": "装置级" };
            descLines.push("事件级别：" + (levelMap[String(result.event_level)] || "未知"));
        }
        if (result.event_sys_type !== undefined) descLines.push("系统类型：" + result.event_sys_type);
        if (result.event_sys_addr !== undefined) descLines.push("系统地址：" + result.event_sys_addr);
        if (result.event_comp_type !== undefined) descLines.push("部件类型：" + result.event_comp_type);
        if (result.event_comp_addr !== undefined) descLines.push("部件地址：" + result.event_comp_addr);
    }

    // 故障事件参数展开
    if (msg.identifier === "fault_event") {
        if (result.fault_type !== undefined) {
            var faultMap = { "1": "主电故障", "2": "备电故障", "5": "总线故障", "6": "系统/装置故障", "7": "部件故障" };
            descLines.push("故障类型：" + (faultMap[String(result.fault_type)] || "未知故障"));
        }
        if (result.fault_sys_type !== undefined) descLines.push("系统类型：" + result.fault_sys_type);
        if (result.fault_sys_addr !== undefined) descLines.push("系统地址：" + result.fault_sys_addr);
    }

    // 电源故障参数展开
    if (msg.identifier === "power_fault_event") {
        if (result.power_type !== undefined) {
            var powerMap = { "1": "主电", "2": "备电", "3": "部件电源" };
            descLines.push("电源类型：" + (powerMap[String(result.power_type)] || "未知"));
        }
        if (result.source_type !== undefined) {
            var sourceMap = { "1": "系统", "2": "部件", "21": "装置" };
            descLines.push("来源类型：" + (sourceMap[String(result.source_type)] || "未知"));
        }
    }

    // PRT 参数展开
    if (msg.identifier === "prt_alarm_event") {
        if (result.device_name) descLines.push("设备名称：" + result.device_name);
        if (result.location) descLines.push("设备位置：" + result.location);
        if (result.alarm_info) descLines.push("报警信息：" + result.alarm_info);
    }

    // CRT 参数展开
    if (msg.identifier === "crt_alarm_event") {
        if (result.alarm_type_desc) descLines.push("报警类型：" + result.alarm_type_desc);
        if (result.linked_device_name) descLines.push("关联子设备：" + result.linked_device_name);
    }

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
分类：${alertCategoryName}
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
${alertCategoryName}：${deviceName}(${deviceDn})${eventTime}发生${eventTypeName}，${content}【消防监控】
```

**邮件主题：**
```
【${alertCategoryName}】【${eventTypeName}】${deviceName} - ${eventTime}
```

## 3. 占位符速查表

| 分类 | 占位符 | 示例值 |
|------|--------|--------|
| 设备基础 | `${deviceName}` `${deviceDn}` `${deviceId}` `${productKey}` `${deviceModel}` `${deviceAddr}` `${deviceState}` | 1046945061FF / 在线 |
| 报警分类 | `${alertCategory}` `${alertCategoryName}` `${alertIcon}` | 1 / 火警 / 🔥 |
| 事件信息 | `${eventTypeName}` `${eventTime}` `${eventIdentifier}` | 火警 / 2025-04-27 15:30:00 |
| 告警聚合 | `${alarmItems}` `${alarmDetail}` `${hasAlarm}` | 装置火警、部件故障 |
| 通用内容 | `${title}` `${content}` `${description}` `${locationInfo}` | 自动生成的标题和内容 |
| 部件专用 | `${isComponentDevice}` `${compParentAddr}` `${compSysType}` `${compSysAddr}` `${compType}` `${compAddr}` `${componentDesc}` | 1 / 1046945061FF / 烟感探测器 |
| 火警参数 | `${event_level}` `${event_sys_type}` `${event_sys_addr}` `${event_comp_type}` `${event_comp_addr}` | 系统级 / 1 / 1 |
| 故障参数 | `${fault_type}` `${fault_sys_type}` `${fault_sys_addr}` | 主电故障 / 1 / 1 |
| 电源参数 | `${power_type}` `${source_type}` | 主电 / 系统 |
| PRT 参数 | `${device_name}` `${location}` `${alarm_info}` | 机器01 / 回路01 / 火警报警 |
| CRT 参数 | `${alarm_type}` `${alarm_type_desc}` `${linked_device_name}` | 1 / 火警 / 1046945061FF_0_0_1_1 |
| 原始参数 | `${dev_fire_alarm}` `${dev_fault}` `${dev_main_power_fault}` `${dev_backup_power_fault}` `${comp_fire_alarm}` `${comp_fault}` `${comp_power_fault}` | 1 / 0 |

## 4. 规则配置对照表

| 序号 | 规则名称 | 监听类型 | 监听标识 | 过滤条件 | 触发场景 |
|------|---------|---------|---------|---------|---------|
| 1 | 传输装置离线 | `event` | `offline` | — | 父设备离线 |
| 2 | 传输装置火警 | `event` | `fire_alarm_event` | — | Type 1/21 系统/装置火警 |
| 3 | 传输装置故障 | `event` | `fault_event` | — | Type 1/21 系统/装置故障 |
| 4 | 传输装置电源故障 | `event` | `power_fault_event` | — | Type 1/21 电源故障 |
| 5 | PRT打印报警 | `event` | `prt_alarm_event` | — | Type 9 PRT点位表报警 |
| 6 | CRT图形报警 | `event` | `crt_alarm_event` | — | Type 10 CRT部件报警 |
| 7 | 部件子设备火警 | `property` | `report` | `comp_fire_alarm == 1` | 部件子设备火警属性 |
| 8 | 部件子设备故障 | `property` | `report` | `comp_fault == 1` | 部件子设备故障属性 |
| 9 | 部件子设备电源故障 | `property` | `report` | `comp_power_fault == 1` | 部件子设备电源故障属性 |

## 5. 使用说明

1. 在规则引擎的"脚本动作"中粘贴上述 `translate` 函数
2. 在规则引擎的"消息模板"中粘贴通用模板
3. 所有 9 条规则**共用同一个脚本和模板**
4. 脚本会自动识别是事件触发还是属性触发，并生成对应的标题和内容
5. `alertCategory` 字段值（1-5）与前端报警配置页面完全对齐，可直接用于分类筛选
