-- 设备地址直接关联空间管理：eiot_device_info 增加 space_id（关联 eiot_space.id）
-- addr 保留为空间字段快照（楼栋+楼层+区域），供告警文案/巡检等既有链路使用
ALTER TABLE `eiot_device_info`
    ADD COLUMN `space_id` bigint(20) NULL COMMENT '空间ID（关联 eiot_space.id，设备地址）' AFTER `addr`,
    ADD INDEX `idx_space_id` (`space_id`);