-- ============================================================
-- 摄像头报警需求 - 数据库变更脚本
-- 日期：2026-08-05
-- ============================================================

-- 1. eiot_alert_record 新增 handle_status 字段
--    0=待处置, 1=稍后处置, 2=处置中, 3=已处置
--    兼容旧数据：readFlg=true 等价于 handleStatus>=2
ALTER TABLE `eiot_alert_record`
    ADD COLUMN `handle_status` tinyint NULL DEFAULT 0 COMMENT '处置状态：0=待处置,1=稍后处置,2=处置中,3=已处置' AFTER `alert_image`;

-- 将历史已读数据标记为已处置(3)
UPDATE `eiot_alert_record` SET `handle_status` = 3 WHERE `read_flg` = 1 AND (`handle_status` IS NULL OR `handle_status` = 0);

-- 2. eiot_device_info 新增 device_type + visible 字段
ALTER TABLE `eiot_device_info`
    ADD COLUMN `device_type` varchar(32) NULL DEFAULT NULL COMMENT '设备类型：transfer/camera/smoke/temp_humidity/door_magnet等' AFTER `node_type`,
    ADD COLUMN `visible` bit(1) NULL DEFAULT b'1' COMMENT '是否显示：设备控制室查询时过滤，为false时不返回' AFTER `device_type`;
