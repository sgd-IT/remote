this.translate = function(msg, device) {
    var result = {};
    // ========== 设备基础信息 ==========
    result.deviceName = device.name || device.dn || "未知设备";
    result.deviceDn = device.dn || "";
    result.deviceId = device.id || "";
    result.productKey = device.productKey || msg.productKey || "";
    result.deviceAddr = device.addr || "";

    // ========== 事件/属性参数扁平化 ==========
    if (msg.data && typeof msg.data === 'object') {
        var keys = Object.keys(msg.data);
        for (var i = 0; i < keys.length; i++) {
            var k = keys[i];
            result[k] = msg.data[k];
        }
    }
    return result;
};