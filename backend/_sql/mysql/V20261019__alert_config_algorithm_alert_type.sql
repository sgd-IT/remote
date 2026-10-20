-- ============================================================
-- 需求：告警中心-告警配置 新增"算法报警类型"可选字段
--   1=高温事件报警 2=热成像事件报警 3=烟火事件报警
--   4=离岗事件报警 5=消防通道占用事件报警；NULL=通用规则配置
--   选择算法报警类型的配置不绑定规则引擎，rule_info_id 放宽为可空
-- 适用库：MySQL
-- 日期：2026-10-19
-- ============================================================

ALTER TABLE `eiot_alert_config`
    ADD COLUMN `algorithm_alert_type` tinyint NULL DEFAULT NULL
        COMMENT '算法报警类型:1=高温事件报警 2=热成像事件报警 3=烟火事件报警 4=离岗事件报警 5=消防通道占用事件报警;NULL=通用规则配置'
        AFTER `rule_info_id`;

ALTER TABLE `eiot_alert_config`
    MODIFY COLUMN `rule_info_id` bigint NULL DEFAULT NULL
        COMMENT '规则引擎id（未选择算法报警类型时必填）';
