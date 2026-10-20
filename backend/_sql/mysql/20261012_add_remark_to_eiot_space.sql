-- eiot_space（空间管理）增加备注字段
-- remark 用于兜底表达夹层、别名、期数等特例说明
ALTER TABLE `eiot_space` ADD COLUMN `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `area`;
