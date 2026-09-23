-- 报警配置表新增字段：消息推送岗位（JSON数组，最多3个岗位ID）
ALTER TABLE `eiot_alert_config`
    ADD COLUMN `push_post_ids` varchar(255) NULL DEFAULT NULL COMMENT '消息推送岗位ID数组(JSON)' AFTER `status`;

-- 系统用户表新增字段：是否允许消息推送（0关 1开，默认关）
ALTER TABLE `system_users`
    ADD COLUMN `push_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否允许消息推送（0关 1开）' AFTER `status`;
