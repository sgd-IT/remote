-- ==========================================
-- 摄像头报警功能 - 数据库变更脚本
-- 日期: 2026-08-05
-- 说明: 为 eiot_alert_record 表添加报警图片字段
-- ==========================================

-- 1. 为告警记录表添加报警图片字段（用于存储摄像头抓拍图片）
ALTER TABLE `eiot_alert_record`
    ADD COLUMN `alert_image` varchar(500) NULL COMMENT '报警图片URL' AFTER `name`;
