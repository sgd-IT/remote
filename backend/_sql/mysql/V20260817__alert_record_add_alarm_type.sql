-- eiot_alert_record 增加报警类型字段
-- 0=主机报警 1=高温事件报警 2=热成像事件报警 3=烟火事件报警 4=离岗事件报警 5=消防通道占用事件报警
ALTER TABLE eiot_alert_record ADD COLUMN alarm_type tinyint NOT NULL DEFAULT 0 COMMENT '报警类型：0=主机报警 1=高温事件报警 2=热成像事件报警 3=烟火事件报警 4=离岗事件报警 5=消防通道占用事件报警' AFTER details;
