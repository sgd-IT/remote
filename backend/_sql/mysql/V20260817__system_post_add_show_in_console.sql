-- system_post 岗位表新增字段：是否展示在远程控制中心
ALTER TABLE `system_post`
    ADD COLUMN `show_in_console` tinyint NOT NULL DEFAULT 0 COMMENT '是否展示在远程控制中心（0否 1是）' AFTER `status`;
