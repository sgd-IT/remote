-- UDP 组件 JSON 配置添加 centerAddr（平台中心地址）
-- 影响: 大华协议下行帧的源地址字段
UPDATE `eiot_component`
SET `config` = JSON_SET(`config`, '$.centerAddr', '020000000001')
WHERE `type` = 'udp' AND `deleted` = 0;
