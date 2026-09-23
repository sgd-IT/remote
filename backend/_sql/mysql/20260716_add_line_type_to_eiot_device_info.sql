-- 设备表新增 line_type 字段：线路类型（0-总线, 1-多线）
ALTER TABLE `eiot_device_info`
    ADD COLUMN `line_type` int NULL DEFAULT NULL COMMENT '线路类型(0-总线, 1-多线)'
    AFTER `node_type`;
