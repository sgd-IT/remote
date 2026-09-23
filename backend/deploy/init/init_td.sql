-- ========================================
-- TDengine 初始化脚本
-- 数据库: aw_fire_remote_monitor
-- 首次启动 TDengine 容器后手动执行:
--   docker exec anwei-tdengine taos -s "source /init/init_td.sql"
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS aw_fire_remote_monitor 
  PRECISION 'ms'
  KEEP 3650
  BUFFER 256
  WAL_LEVEL 1;

USE aw_fire_remote_monitor;

-- 父设备属性超级表
-- CREATE STABLE IF NOT EXISTS product_property_parent (ts TIMESTAMP, ...) TAGS (device_id NCHAR(64));
-- 部件子设备属性超级表
-- CREATE STABLE IF NOT EXISTS product_property_child (ts TIMESTAMP, ...) TAGS (device_id NCHAR(64));

-- 注意: 具体超级表结构由应用程序启动时自动创建，此处仅建库
