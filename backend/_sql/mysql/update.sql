ALTER TABLE eiot_alert_record ADD COLUMN device_id bigint DEFAULT NULL COMMENT '设备id';
ALTER TABLE eiot_alert_record ADD COLUMN product_key varchar(255) COMMENT '产品Key';

-- ----------------------------
-- Table structure for media_server
-- ----------------------------
DROP TABLE IF EXISTS `icc_media_server`;
CREATE TABLE `icc_media_server` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    `server_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置名称',
    `base_url` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ICC平台基础地址',
    `client_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '合作方凭证ID',
    `client_secret` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '合作方凭证密钥',
    `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ICC登录用户名',
    `password` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ICC登录密码（RSA加密）',
    `public_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'RSA公钥',
    `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_tenant_id` (`tenant_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'ICC平台配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for video_camera
-- ----------------------------
DROP TABLE IF EXISTS `video_camera`;
CREATE TABLE `video_camera` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `channel_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ICC通道编码',
    `channel_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '摄像头名称',
    `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-在线，0-离线',
    `longitude` double DEFAULT NULL COMMENT '经度',
    `latitude` double DEFAULT NULL COMMENT '纬度',
    `address` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '安装地址',
    `dept_id` bigint DEFAULT NULL COMMENT '所属机构',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_channel_id_tenant` (`channel_id`, `tenant_id`) USING BTREE,
    KEY `idx_dept_id` (`dept_id`) USING BTREE,
    KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '摄像头管理表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for device_camera_relation
-- ----------------------------
DROP TABLE IF EXISTS `device_camera_relation`;
CREATE TABLE `device_camera_relation` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `device_id` bigint NOT NULL COMMENT '消防部件设备ID',
    `camera_id` bigint NOT NULL COMMENT '摄像头ID',
    `relation_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'manual' COMMENT '关联类型：manual-手动绑定',
    `bind_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_device_camera` (`device_id`, `camera_id`) USING BTREE,
    KEY `idx_device_id` (`device_id`) USING BTREE,
    KEY `idx_camera_id` (`camera_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '消防部件与摄像头关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 视频监控模块动态菜单配置
-- ----------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3167, '视频监控', '', 1, 20, 0, '/video', 'ep:video-camera', '', '', 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3168, '摄像头管理', 'video:camera-manage:query', 2, 0, 3167, 'camera-manage', 'ep:video-camera', 'eiot/video/camera-manage/index', 'VideoCameraManage', 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3169, '视频管理', 'video:video-manage:query', 2, 1, 3167, 'video-manage', 'ep:video-play', 'eiot/video/video-manage/index', 'VideoManage', 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3170, '部件绑定', 'video:device-bind:query', 2, 2, 3167, 'device-bind', 'ep:link', 'eiot/video/device-bind/index', 'VideoDeviceBind', 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3171, 'ICC平台配置', 'video:media-server:query', 2, 3, 3167, 'media-server', 'ep:setting', 'eiot/video/media-server/index', 'VideoMediaServer', 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3172, '摄像头查询', 'video:camera-manage:query', 3, 1, 3168, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3173, '摄像头创建', 'video:camera-manage:create', 3, 2, 3168, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3174, '摄像头更新', 'video:camera-manage:update', 3, 3, 3168, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3175, '摄像头删除', 'video:camera-manage:delete', 3, 4, 3168, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3176, '视频查询', 'video:video-manage:query', 3, 1, 3169, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3177, '绑定查询', 'video:device-bind:query', 3, 1, 3170, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3178, '绑定创建', 'video:device-bind:create', 3, 2, 3170, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3179, '绑定更新', 'video:device-bind:update', 3, 3, 3170, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3180, '绑定删除', 'video:device-bind:delete', 3, 4, 3170, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3181, '配置查询', 'video:media-server:query', 3, 1, 3171, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3182, '配置创建', 'video:media-server:create', 3, 2, 3171, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3183, '配置更新', 'video:media-server:update', 3, 3, 3171, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3184, '配置删除', 'video:media-server:delete', 3, 4, 3171, '', '', '', NULL, 0, b'1', b'1', b'1', '1', '2026-04-27 00:00:00', '1', '2026-04-27 00:00:00', b'0');

-- ----------------------------
-- 2026-04-27 视频模块 device_id 字段迁移
-- ----------------------------
ALTER TABLE `video_camera`
    ADD COLUMN `device_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备编码/录像机编码' AFTER `channel_id`;

ALTER TABLE eiot_device_info
    ADD COLUMN brand_code VARCHAR(64) NULL COMMENT '品牌代码（四信API用）',
ADD COLUMN api_version VARCHAR(32) NULL COMMENT 'API协议版本（四信API用）';

-- ----------------------------
-- 2026-05-07 设备远程控制模块表
-- ----------------------------

CREATE TABLE IF NOT EXISTS `eiot_brand` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '品牌id',
    `brand_code` varchar(64) NOT NULL COMMENT '品牌代码',
    `name` varchar(64) NOT NULL COMMENT '品牌名称',
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_brand_code` (`brand_code`),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB COMMENT='设备品牌表';

CREATE TABLE IF NOT EXISTS `eiot_brand_model` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '型号id',
    `brand_id` bigint NOT NULL COMMENT '关联品牌id',
    `model` varchar(64) NOT NULL COMMENT '型号',
    `version` varchar(32) NOT NULL COMMENT '版本',
    `sync_time` datetime DEFAULT NULL COMMENT '同步时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_brand_model_version` (`brand_id`, `model`, `version`),
    KEY `idx_brand_id` (`brand_id`)
) ENGINE=InnoDB COMMENT='品牌型号版本表';

CREATE TABLE IF NOT EXISTS `eiot_brand_model_function` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '功能id',
    `brand_model_id` bigint NOT NULL COMMENT '关联品牌型号id',
    `function_id` varchar(64) NOT NULL COMMENT '四信原始功能ID',
    `name` varchar(64) NOT NULL COMMENT '功能名称',
    `rule_num` int DEFAULT NULL COMMENT '规则号',
    `await_time` int DEFAULT NULL COMMENT '等待时间(ms)',
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_function` (`brand_model_id`, `function_id`),
    KEY `idx_brand_model_id` (`brand_model_id`)
) ENGINE=InnoDB COMMENT='型号功能表';

CREATE TABLE IF NOT EXISTS `eiot_brand_model_function_param` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '参数id',
    `function_id` bigint NOT NULL COMMENT '关联型号功能id',
    `param_name` varchar(64) NOT NULL COMMENT '参数名称',
    `param_code` varchar(64) NOT NULL COMMENT '参数代码',
    `param_type` varchar(32) NOT NULL COMMENT '参数类型',
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_function_id` (`function_id`)
) ENGINE=InnoDB COMMENT='功能参数表';

CREATE TABLE IF NOT EXISTS `eiot_device_remote_config` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置id',
    `device_id` bigint NOT NULL COMMENT '关联设备id',
    `brand_model_id` bigint NOT NULL COMMENT '绑定的品牌型号id',
    `confirm_password` varchar(64) DEFAULT NULL COMMENT '二次确认密码',
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_id` (`device_id`),
    KEY `idx_brand_model_id` (`brand_model_id`)
) ENGINE=InnoDB COMMENT='设备远程配置表';

CREATE TABLE IF NOT EXISTS `eiot_device_function` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `device_id` bigint NOT NULL COMMENT '关联设备id',
    `brand_model_function_id` bigint NOT NULL COMMENT '关联型号功能id',
    `button_name` varchar(64) NOT NULL COMMENT '自定义按钮名称',
    `sort_order` int NOT NULL DEFAULT 0 COMMENT '展示顺序',
    `param_values` text COMMENT '参数值JSON',
    `enabled` bit(1) NOT NULL DEFAULT b'1',
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_function` (`device_id`, `brand_model_function_id`),
    KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB COMMENT='设备功能实例表';

-- ----------------------------
-- 2026-05-07 设备远程控制模块权限菜单
-- ----------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3185, '品牌管理', 'iot:brand:query', 2, 4, 2814, 'brand', 'ep:collection-tag', 'eiot/brand/index', 'Brand', 0, b'1', b'1', b'1', '1', '2026-05-07 00:00:00', '1', '2026-05-07 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3186, '品牌查询', 'iot:brand:query', 3, 1, 3185, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-07 00:00:00', '1', '2026-05-07 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3187, '品牌同步', 'iot:brand:sync', 3, 2, 3185, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-07 00:00:00', '1', '2026-05-07 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3188, '远程控制配置', 'iot:device:remote:config', 3, 12, 2827, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-07 00:00:00', '1', '2026-05-07 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3189, '指令下发', 'iot:device:instruction:send', 3, 13, 2827, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-07 00:00:00', '1', '2026-05-07 00:00:00', b'0');

-- ----------------------------
-- 2026-06-22 维保单位管理权限菜单细化
-- 注意：parent_id 需设置为「消防值守」父菜单的 ID，请根据实际数据库查询后替换
-- 查询方式：SELECT id FROM system_menu WHERE name = '消防值守' AND deleted = b'0';
-- 下方使用 @firewatch_parent_id 变量，请先执行 SET 语句
-- ----------------------------
SET @firewatch_parent_id = (SELECT id FROM system_menu WHERE name = '消防值守' AND deleted = b'0' LIMIT 1);

-- 维保单位管理 - 页面菜单 (type=2)
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3191, '维保单位管理', 'eiot:firewatch:maintenance-unit:query', 2, 10, @firewatch_parent_id, 'maintenance-unit', 'ep:office-building', 'eiot/firewatch/maintenance-unit/index', 'FireMaintenanceUnit', 0, b'1', b'1', b'1', '1', '2026-06-22 00:00:00', '1', '2026-06-22 00:00:00', b'0');

-- 维保单位管理 - 按钮权限 (type=3)
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3192, '维保单位查询', 'eiot:firewatch:maintenance-unit:query', 3, 1, 3191, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-06-22 00:00:00', '1', '2026-06-22 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3193, '维保单位创建', 'eiot:firewatch:maintenance-unit:create', 3, 2, 3191, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-06-22 00:00:00', '1', '2026-06-22 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3194, '维保单位更新', 'eiot:firewatch:maintenance-unit:update', 3, 3, 3191, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-06-22 00:00:00', '1', '2026-06-22 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3195, '维保单位删除', 'eiot:firewatch:maintenance-unit:delete', 3, 4, 3191, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-06-22 00:00:00', '1', '2026-06-22 00:00:00', b'0');

-- ----------------------------
-- 2026-06-22 维保计划管理权限菜单细化
-- ----------------------------

-- 维保计划管理 - 页面菜单 (type=2)
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3196, '维保计划管理', 'eiot:firewatch:plan:query', 2, 11, @firewatch_parent_id, 'plan', 'ep:calendar', 'eiot/firewatch/plan/index', 'FireMaintenancePlan', 0, b'1', b'1', b'1', '1', '2026-06-22 00:00:00', '1', '2026-06-22 00:00:00', b'0');

-- 维保计划管理 - 按钮权限 (type=3)
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3197, '维保计划查询', 'eiot:firewatch:plan:query', 3, 1, 3196, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-06-22 00:00:00', '1', '2026-06-22 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3198, '维保计划创建', 'eiot:firewatch:plan:create', 3, 2, 3196, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-06-22 00:00:00', '1', '2026-06-22 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3199, '维保计划更新', 'eiot:firewatch:plan:update', 3, 3, 3196, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-06-22 00:00:00', '1', '2026-06-22 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3200, '维保计划删除', 'eiot:firewatch:plan:delete', 3, 4, 3196, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-06-22 00:00:00', '1', '2026-06-22 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3201, '维保计划导出', 'eiot:firewatch:plan:export', 3, 5, 3196, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-06-22 00:00:00', '1', '2026-06-22 00:00:00', b'0');

-- ----------------------------
-- 2026-06-22 维保合同管理权限菜单细化
-- ----------------------------

-- 维保合同管理 - 页面菜单 (type=2)
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3202, '维保合同管理', 'eiot:firewatch:contract:query', 2, 12, @firewatch_parent_id, 'contract', 'ep:document', 'eiot/firewatch/contract/index', 'FireMaintenanceContract', 0, b'1', b'1', b'1', '1', '2026-06-22 00:00:00', '1', '2026-06-22 00:00:00', b'0');

-- 维保合同管理 - 按钮权限 (type=3)
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3203, '维保合同查询', 'eiot:firewatch:contract:query', 3, 1, 3202, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-06-22 00:00:00', '1', '2026-06-22 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3204, '维保合同创建', 'eiot:firewatch:contract:create', 3, 2, 3202, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-06-22 00:00:00', '1', '2026-06-22 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3205, '维保合同更新', 'eiot:firewatch:contract:update', 3, 3, 3202, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-06-22 00:00:00', '1', '2026-06-22 00:00:00', b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (3206, '维保合同删除', 'eiot:firewatch:contract:delete', 3, 4, 3202, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-06-22 00:00:00', '1', '2026-06-22 00:00:00', b'0');

