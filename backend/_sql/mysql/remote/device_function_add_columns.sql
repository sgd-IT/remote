ALTER TABLE `eiot_device_function`
    ADD COLUMN `sub_device_id` BIGINT DEFAULT NULL COMMENT '绑定的子设备ID',
    ADD COLUMN `function_type` VARCHAR(32) NOT NULL DEFAULT 'GENERAL' COMMENT '功能类型：BUS(总线按钮)/MULTI(多线按钮)/GENERAL(通用按钮)';
