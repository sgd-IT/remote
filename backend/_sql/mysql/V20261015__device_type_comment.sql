-- ============================================================
-- eiot_device_info.device_type 字段注释补全
-- 日期：2026-10-15
-- 背景：原 DDL 注释仅列了 5 个示例值，现按代码实际使用补全完整枚举
-- ============================================================

-- device_type 完整取值表（eiot_device_info.device_type）：
--   transfer       用传设备（主机/网关，findTransferDeviceId 唯一识别的类型，用于远程控制跳转）
--   camera         摄像头（视频类告警来源：烟火/热成像/高温/离岗/通道占用）
--   smoke          烟感探测器（AEP 烟感链路主力设备）
--   temperature    温感探测器
--   temp_humidity  温湿度传感器
--   door_magnet    门磁传感器
--   gas            燃气探测器
--   hydrant        消火栓
--   manual         手动报警器
--   NULL           未分类（历史存量数据多为此值）
--
-- 注意：device_type 是"物理设备分类"，与 alarm_kind（告警类型：火警/隐患/故障等）正交。
--       findTransferDeviceId 只认 "transfer"，其余类型一律走父链上溯或摄像头兜底。

ALTER TABLE `eiot_device_info`
    MODIFY COLUMN `device_type` varchar(32) NULL DEFAULT NULL
    COMMENT '设备类型：transfer=用传设备,camera=摄像头,smoke=烟感,temperature=温感,temp_humidity=温湿度,door_magnet=门磁,gas=燃气,hydrant=消火栓,manual=手报,NULL=未分类'
    AFTER `node_type`;
