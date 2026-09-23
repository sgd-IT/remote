-- ====================================================================
-- 远程控制多协议兼容: 协议厂商配置表 + 厂商指令字典表
-- 品牌表关联厂商 / 品牌型号功能关联厂商指令
-- ====================================================================

-- 1. 协议厂商配置表(全局配置表, 已加入 tenant ignore-tables)
CREATE TABLE IF NOT EXISTS `eiot_protocol_vendor`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vendor_code`  VARCHAR(32)  NOT NULL COMMENT '厂商编码: FOURFAITH/YWN/...',
    `vendor_name`  VARCHAR(64)  NOT NULL COMMENT '厂商名称',
    `invoke_type`  VARCHAR(32)  NOT NULL COMMENT '调用逻辑: REMOTE_API=远程API换码(四信类) LOCAL_COMMAND=本地命令码映射(YWN类)',
    `service_name` VARCHAR(64)  NOT NULL DEFAULT 'remote_control' COMMENT '统一下发服务名',
    `config`       TEXT                  DEFAULT NULL COMMENT '厂商级配置JSON',
    `remark`       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME              DEFAULT NULL COMMENT '更新时间',
    `creator`      VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `updater`      VARCHAR(64)           DEFAULT NULL COMMENT '更新者',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vendor_code` (`vendor_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '私有协议厂商配置';

-- 2. 厂商指令字典表(LOCAL_COMMAND 类厂商使用, 全局配置表)
CREATE TABLE IF NOT EXISTS `eiot_vendor_command`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vendor_code`   VARCHAR(32)  NOT NULL COMMENT '厂商编码',
    `command_key`   VARCHAR(64)  NOT NULL COMMENT '指令标识: reset/bus_switch/...',
    `command_name`  VARCHAR(64)  NOT NULL COMMENT '指令名称',
    `function_type` VARCHAR(16)  NOT NULL DEFAULT 'GENERAL' COMMENT '按钮类型: GENERAL/BUS/MULTI',
    `code_start`    INT          NOT NULL COMMENT '启动/执行命令码',
    `code_stop`     INT                   DEFAULT NULL COMMENT '停止命令码(无启停语义则为NULL)',
    `param_schema`  TEXT                  DEFAULT NULL COMMENT '参数模板JSON: {"params":[{"code":"loopNo","name":"回路号","slot":"param1","default":0}]}, slot取值 machineNo/param1/param2',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME              DEFAULT NULL COMMENT '更新时间',
    `creator`       VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `updater`       VARCHAR(64)           DEFAULT NULL COMMENT '更新者',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vendor_command` (`vendor_code`, `command_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '厂商指令字典';

-- 3. 品牌表关联厂商(为空时后端默认按 FOURFAITH 处理, 向后兼容)
ALTER TABLE `eiot_brand`
    ADD COLUMN `vendor_code` VARCHAR(32) DEFAULT NULL COMMENT '协议厂商编码, 关联 eiot_protocol_vendor.vendor_code';

-- 4. 品牌型号功能关联厂商指令(LOCAL_COMMAND 类厂商使用)
ALTER TABLE `eiot_brand_model_function`
    ADD COLUMN `vendor_command_key` VARCHAR(64) DEFAULT NULL COMMENT '厂商指令标识, 关联 eiot_vendor_command.command_key';

-- ====================================================================
-- 种子数据
-- ====================================================================

-- 厂商: 四信(远程API换码) / 运维牛(本地命令码映射)
INSERT INTO `eiot_protocol_vendor` (`vendor_code`, `vendor_name`, `invoke_type`, `service_name`, `remark`)
VALUES ('FOURFAITH', '四信', 'REMOTE_API', 'remote_control', '调用四信云平台API生成指令码后下发'),
       ('YWN', '运维牛', 'LOCAL_COMMAND', 'remote_control', '本地命令码映射, 组件统一 remote_control 服务(command_code/machineNo/param1/param2)');

-- 存量四信品牌回填厂商编码
UPDATE `eiot_brand` SET `vendor_code` = 'FOURFAITH' WHERE `vendor_code` IS NULL;

-- YWN 指令字典(命令码见协议文档: 0x02复位 0x03消音 0x04/0x05总线启停 0x06手自动转换
-- 0x07屏蔽 0x08取消屏蔽 0x0A/0x0B多线启停 0x0C自动允许 0x0D自动禁止 0x0E手动允许 0x0F手动禁止 0x10自动手动允许 0x11自动手动禁止)
-- 注意: GENERAL 类型按钮无启停操作, 带启停语义的指令拆为启动/停止各一条记录(code_stop=NULL)
INSERT INTO `eiot_vendor_command` (`vendor_code`, `command_key`, `command_name`, `function_type`, `code_start`, `code_stop`, `param_schema`)
VALUES ('YWN', 'reset', '复位', 'GENERAL', 2, NULL,
        '{"params":[{"code":"machineNo","name":"机号","slot":"machineNo","default":1}]}'),
       ('YWN', 'silence', '消音', 'GENERAL', 3, NULL,
        '{"params":[{"code":"machineNo","name":"机号","slot":"machineNo","default":1}]}'),
       ('YWN', 'manual_auto_switch', '手自动转换', 'GENERAL', 6, NULL,
        '{"params":[{"code":"machineNo","name":"机号","slot":"machineNo","default":1},{"code":"mode","name":"手自动模式","slot":"param1","default":0}]}'),
       ('YWN', 'bus_switch', '总线开关', 'BUS', 4, 5,
        '{"params":[{"code":"machineNo","name":"机号","slot":"machineNo","default":1},{"code":"loopNo","name":"回路号","slot":"param1","default":0},{"code":"addrNo","name":"地址号","slot":"param2","default":0}]}'),
       ('YWN', 'shield', '屏蔽', 'GENERAL', 7, NULL,
        '{"params":[{"code":"machineNo","name":"机号","slot":"machineNo","default":1},{"code":"loopNo","name":"回路号","slot":"param1","default":0},{"code":"addrNo","name":"地址号","slot":"param2","default":0}]}'),
       ('YWN', 'unshield', '取消屏蔽', 'GENERAL', 8, NULL,
        '{"params":[{"code":"machineNo","name":"机号","slot":"machineNo","default":1},{"code":"loopNo","name":"回路号","slot":"param1","default":0},{"code":"addrNo","name":"地址号","slot":"param2","default":0}]}'),
       ('YWN', 'multi_line_switch', '多线开关', 'MULTI', 10, 11,
        '{"params":[{"code":"machineNo","name":"机号","slot":"machineNo","default":1},{"code":"panelNo","name":"盘号","slot":"param1","default":0},{"code":"lineNo","name":"路号","slot":"param2","default":0}]}'),
       ('YWN', 'auto_allow', '自动允许', 'GENERAL', 12, NULL,
        '{"params":[{"code":"machineNo","name":"机号","slot":"machineNo","default":1}]}'),
       ('YWN', 'auto_forbid', '自动禁止', 'GENERAL', 13, NULL,
        '{"params":[{"code":"machineNo","name":"机号","slot":"machineNo","default":1}]}'),
       ('YWN', 'manual_allow', '手动允许', 'GENERAL', 14, NULL,
        '{"params":[{"code":"machineNo","name":"机号","slot":"machineNo","default":1}]}'),
       ('YWN', 'manual_forbid', '手动禁止', 'GENERAL', 15, NULL,
        '{"params":[{"code":"machineNo","name":"机号","slot":"machineNo","default":1}]}'),
       ('YWN', 'auto_manual_allow', '自动手动允许', 'GENERAL', 16, NULL,
        '{"params":[{"code":"machineNo","name":"机号","slot":"machineNo","default":1}]}'),
       ('YWN', 'auto_manual_forbid', '自动手动禁止', 'GENERAL', 17, NULL,
        '{"params":[{"code":"machineNo","name":"机号","slot":"machineNo","default":1}]}');

-- ====================================================================
-- YWN 品牌/型号/功能种子(品牌管理仅能同步四信数据, YWN 需手工种子)
-- 远程控制设置弹窗的品牌型号级联依赖 eiot_brand_model, 缺失则无叶子节点可选
-- ====================================================================

-- 品牌: 运维牛(vendor_code=YWN)
INSERT INTO `eiot_brand` (`brand_code`, `name`, `vendor_code`)
VALUES ('YWN', '运维牛', 'YWN');

-- 型号: 运维牛装置 V27
INSERT INTO `eiot_brand_model` (`brand_id`, `model`, `version`, `sync_time`)
SELECT b.`id`, '运维牛装置', 'V27', NOW()
FROM `eiot_brand` b WHERE b.`brand_code` = 'YWN';

-- 型号功能: 绑定厂商指令字典 command_key(function_id 四信专用, YWN 填 command_key 占位)
INSERT INTO `eiot_brand_model_function` (`brand_model_id`, `function_id`, `name`, `vendor_command_key`)
SELECT m.`id`, t.`command_key`, t.`name`, t.`command_key`
FROM `eiot_brand_model` m
JOIN `eiot_brand` b ON b.`id` = m.`brand_id` AND b.`brand_code` = 'YWN'
JOIN (
    SELECT 'reset' AS command_key, '复位' AS name
    UNION ALL SELECT 'silence', '消音'
    UNION ALL SELECT 'manual_auto_switch', '手自动转换'
    UNION ALL SELECT 'bus_switch', '总线开关'
    UNION ALL SELECT 'shield', '屏蔽'
    UNION ALL SELECT 'unshield', '取消屏蔽'
    UNION ALL SELECT 'multi_line_switch', '多线开关'
    UNION ALL SELECT 'auto_allow', '自动允许'
    UNION ALL SELECT 'auto_forbid', '自动禁止'
    UNION ALL SELECT 'manual_allow', '手动允许'
    UNION ALL SELECT 'manual_forbid', '手动禁止'
    UNION ALL SELECT 'auto_manual_allow', '自动手动允许'
    UNION ALL SELECT 'auto_manual_forbid', '自动手动禁止'
) t
WHERE m.`model` = '运维牛装置';
