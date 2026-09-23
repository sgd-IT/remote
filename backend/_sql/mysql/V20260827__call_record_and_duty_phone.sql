-- ======================================================================
-- 通话录音中心：值守中心座机号码表 + 话单通话记录表 + 菜单权限
-- 需求：值守平台（系统租户）录入值守中心座机电话（归属固定为系统租户），
--       话单推送接口按号码匹配主/被叫，另一号码匹配系统用户手机号，
--       通话记录归属到该用户所属租户（未匹配归系统租户）
-- ======================================================================

-- ----------------------------
-- 表1：值守中心座机号码表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `eiot_duty_phone`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`    BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID（固定为值守中心系统租户1）',
    `phone_number` VARCHAR(50)  NOT NULL COMMENT '座机电话号码',
    `name`         VARCHAR(100)          DEFAULT NULL COMMENT '座机名称（如：XX值班中心前台）',
    `remark`       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME              DEFAULT NULL COMMENT '更新时间',
    `creator`      VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `updater`      VARCHAR(64)           DEFAULT NULL COMMENT '更新者',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_phone_number` (`phone_number`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '值守中心座机号码';

-- ----------------------------
-- 表2：话单通话记录表（对应《话单推送接口文档 V2.6》27个字段 + 匹配扩展字段）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `eiot_call_record`
(
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`          BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID（另一号码匹配系统用户手机号，归该用户所属租户；未匹配归系统租户1）',
    `session_id`         VARCHAR(200) NOT NULL COMMENT '通话唯一标志（话单sessionid，幂等键）',
    `display_nbr`        VARCHAR(50)           DEFAULT NULL COMMENT '显示号码',
    `caller_nbr`         VARCHAR(50)           DEFAULT NULL COMMENT '主叫号码',
    `called_nbr`         VARCHAR(50)           DEFAULT NULL COMMENT '被叫号码',
    `service_nbr`        VARCHAR(50)           DEFAULT NULL COMMENT '业务号码',
    `service_nbr_b`      VARCHAR(50)           DEFAULT NULL COMMENT '业务号码B',
    `dial_time`          VARCHAR(30)           DEFAULT NULL COMMENT '拨打时间(YYYYMMDDHH24MISS)',
    `ring_time`          VARCHAR(30)           DEFAULT NULL COMMENT '振铃时间(YYYYMMDDHH24MISS)',
    `start_time`         VARCHAR(30)           DEFAULT NULL COMMENT '开始时间(YYYYMMDDHH24MISS)',
    `end_time`           VARCHAR(30)           DEFAULT NULL COMMENT '结束时间(YYYYMMDDHH24MISS)',
    `duration`           VARCHAR(10)           DEFAULT NULL COMMENT '通话时长(秒)',
    `min_duration`       VARCHAR(10)           DEFAULT NULL COMMENT '通话时长(分钟)',
    `service_type`       VARCHAR(50)           DEFAULT NULL COMMENT '话单类型(yly-caller/yly-callee/yzj-caller/yzj-callee等)',
    `disposition`        VARCHAR(45)           DEFAULT NULL COMMENT '通话响应状态(NO ANSWER/FAILED/BUSY/ANSWERED/CONGESTION)',
    `voice_address`      VARCHAR(500)          DEFAULT NULL COMMENT '混合录音文件地址',
    `voice_address_in`   VARCHAR(500)          DEFAULT NULL COMMENT '主叫一方录音文件地址',
    `voice_address_out`  VARCHAR(500)          DEFAULT NULL COMMENT '被叫一方录音文件地址',
    `md5_value`          VARCHAR(64)           DEFAULT NULL COMMENT '混合录音文件md5值',
    `md5_value_in`       VARCHAR(64)           DEFAULT NULL COMMENT '主叫录音文件md5值',
    `md5_value_out`      VARCHAR(64)           DEFAULT NULL COMMENT '被叫录音文件md5值',
    `extension_num`      VARCHAR(50)           DEFAULT NULL COMMENT '分机号',
    `user_data`          VARCHAR(64)           DEFAULT NULL COMMENT '用户自定义数据',
    `call_in`            VARCHAR(30)           DEFAULT NULL COMMENT '上平台时间(YYYY-MM-DD HH:MM:SS)',
    `answer`             VARCHAR(30)           DEFAULT NULL COMMENT '应答时刻(YYYY-MM-DD HH:MM:SS)',
    `hung_up`            VARCHAR(30)           DEFAULT NULL COMMENT '挂机时刻(YYYY-MM-DD HH:MM:SS)',
    `hung_up_originator` VARCHAR(10)           DEFAULT NULL COMMENT '挂断事件发起方(0主叫 1被叫)',
    `start_ring`         VARCHAR(30)           DEFAULT NULL COMMENT '响铃时刻(YYYY-MM-DD HH:MM:SS)',
    `match_phone`        VARCHAR(50)           DEFAULT NULL COMMENT '匹配到的值守中心座机号码（未匹配为空）',
    `call_direction`     TINYINT      NOT NULL DEFAULT 0 COMMENT '通话方向：0=未匹配 1=呼出(归属方为主叫) 2=呼入(归属方为被叫)',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        DATETIME              DEFAULT NULL COMMENT '更新时间',
    `creator`            VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `updater`            VARCHAR(64)           DEFAULT NULL COMMENT '更新者',
    `deleted`            TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`),
    KEY `idx_tenant_id_start_time` (`tenant_id`, `start_time`),
    KEY `idx_caller_nbr` (`caller_nbr`),
    KEY `idx_called_nbr` (`called_nbr`),
    KEY `idx_match_phone` (`match_phone`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '话单通话记录';

-- ----------------------------
-- 菜单权限注册
-- 注意：parent_id 为「消防值守」父菜单 ID，执行前自动查询
-- 菜单ID分配（延续 firewatch 模块序列，本次从 3218 开始）：
--   3218-3222：值守中心座机管理（页面 + 查询/创建/更新/删除按钮）
--   3223-3225：通话记录（页面 + 查询按钮 + 录音下载按钮）
-- ----------------------------
SET @firewatch_parent_id = (SELECT id FROM system_menu WHERE name = '消防值守' AND deleted = 0 LIMIT 1);

-- 值守中心座机管理 - 页面菜单 (type=2)
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3218, '值守中心座机管理', 'eiot:firewatch:duty-phone:query', 2, 13, @firewatch_parent_id, 'duty-phone', 'ep:phone', 'eiot/firewatch/call-center/phone/index', 'FireDutyPhone', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

-- 值守中心座机管理 - 按钮权限 (type=3)
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3219, '座机号码查询', 'eiot:firewatch:duty-phone:query', 3, 1, 3218, '', '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3220, '座机号码创建', 'eiot:firewatch:duty-phone:create', 3, 2, 3218, '', '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3221, '座机号码更新', 'eiot:firewatch:duty-phone:update', 3, 3, 3218, '', '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3222, '座机号码删除', 'eiot:firewatch:duty-phone:delete', 3, 4, 3218, '', '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

-- 通话记录 - 页面菜单 (type=2)
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3223, '通话记录', 'eiot:firewatch:call-record:query', 2, 14, @firewatch_parent_id, 'call-record', 'ep:microphone', 'eiot/firewatch/call-center/record/index', 'FireCallRecord', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

-- 通话记录 - 按钮权限 (type=3)
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3224, '通话记录查询', 'eiot:firewatch:call-record:query', 3, 1, 3223, '', '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3225, '通话录音下载', 'eiot:firewatch:call-record:voice', 3, 2, 3223, '', '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

-- ======================================================================
-- 说明：以上菜单 SQL 执行后，需在【租户管理-套餐权限】中为相关角色勾选新菜单权限
-- ======================================================================
