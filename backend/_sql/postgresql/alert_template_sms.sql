
-- 增加短信告警模板
alter table eiot_channel_template add column status int4 default 1;
alter table eiot_channel_template add column template_code varchar(128);
comment on column eiot_channel_template.status is '状态 0-待审核 1-审核成功 2-审核失败';
comment on column eiot_channel_template.template_code is '模板编号';
        

INSERT INTO channel VALUES (4, 'SMS', NULL, '短信', '', '2025-02-06 21:43:55', '', '2025-02-07 20:53:03', '0', 1, 0);
INSERT INTO channel VALUES (5, 'VMS', NULL, '语音', '', '2025-02-06 21:43:55', '', '2025-02-07 20:53:03', '0', 1, 0);



-- 增加告警记录字段     
alter table eiot_alert_record add column device_id int8 null;
alter table eiot_alert_record add column product_key varchar(64);
comment on column eiot_alert_record.device_id is '设备id';
comment on column eiot_alert_record.product_key is '产品key';