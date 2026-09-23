CREATE TABLE IF NOT EXISTS `eiot_remote_operation_log`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID',
    `device_id`       BIGINT       NOT NULL COMMENT '设备ID',
    `device_name`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '设备名称',
    `property_id`     BIGINT                DEFAULT NULL COMMENT '企业/物业ID',
    `property_name`   VARCHAR(64)           DEFAULT NULL COMMENT '企业/物业名称',
    `operation_type`  VARCHAR(32)  NOT NULL COMMENT '操作类型：MUTE/RESET/SWITCH_MODE/START/STOP',
    `operation_name`  VARCHAR(64)  NOT NULL COMMENT '操作名称',
    `button_name`     VARCHAR(64)           DEFAULT NULL COMMENT '按钮名称',
    `function_type`   VARCHAR(16)           DEFAULT NULL COMMENT '功能类型：BUS(总线)/MULTI(多线)/GENERAL(通用)',
    `operator_id`     BIGINT       NOT NULL COMMENT '操作人ID',
    `operator_name`   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '操作人姓名',
    `reviewer_id`     BIGINT                DEFAULT NULL COMMENT '复核人ID',
    `reviewer_name`   VARCHAR(64)           DEFAULT NULL COMMENT '复核人姓名',
    `command_status`  TINYINT      NOT NULL DEFAULT 0 COMMENT '命令状态：0=失败 1=成功 2=执行中',
    `result_msg`      VARCHAR(512)          DEFAULT NULL COMMENT '结果说明',
    `request_params`  TEXT                  DEFAULT NULL COMMENT '请求参数JSON',
    `ip_address`      VARCHAR(45)           DEFAULT NULL COMMENT '操作IP',
    `operation_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME              DEFAULT NULL COMMENT '更新时间',
    `creator`         VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `updater`         VARCHAR(64)           DEFAULT NULL COMMENT '更新者',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_device_id_operation_time` (`device_id`, `operation_time`),
    KEY `idx_tenant_id_operation_time` (`tenant_id`, `operation_time`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_operation_type` (`operation_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '远程控制操作日志';

-- ============================================================
-- 增量升级：已有库需手动执行以下 ALTER 语句补充新列
-- （新建库由上方 CREATE TABLE 直接包含这两列，无需执行）
-- ============================================================
-- ALTER TABLE `eiot_remote_operation_log`
--   ADD COLUMN `button_name` VARCHAR(64) DEFAULT NULL COMMENT '按钮名称' AFTER `operation_name`,
--   ADD COLUMN `function_type` VARCHAR(16) DEFAULT NULL COMMENT '功能类型：BUS(总线)/MULTI(多线)/GENERAL(通用)' AFTER `button_name`;
