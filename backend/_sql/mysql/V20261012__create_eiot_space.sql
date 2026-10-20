-- ======================================================================
-- 空间管理：租户（园区）下的 楼栋 / 楼层 / 区域 三级空间划分表
-- 需求：每行一条空间记录，按租户隔离；供设备、点位等后续关联使用
-- ======================================================================

CREATE TABLE IF NOT EXISTS `eiot_space`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`   BIGINT      NOT NULL DEFAULT 0 COMMENT '租户编号（园区）',
    `building`    VARCHAR(64) NOT NULL COMMENT '楼栋（几号楼，如：1号楼/A栋）',
    `floor`       VARCHAR(32) NOT NULL COMMENT '楼层（几层，如：3层/-1层/3F）',
    `area`        VARCHAR(64) NOT NULL DEFAULT '' COMMENT '区域（如：东区/厨房/配电房）',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME             DEFAULT NULL COMMENT '更新时间',
    `creator`     VARCHAR(64)          DEFAULT NULL COMMENT '创建者',
    `updater`     VARCHAR(64)          DEFAULT NULL COMMENT '更新者',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_space` (`tenant_id`, `building`, `floor`, `area`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '空间管理（楼栋/楼层/区域）';
