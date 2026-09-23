-- 新增区域编码字段，用于数据权限的编码匹配
ALTER TABLE system_tenant ADD COLUMN province_code INT NULL COMMENT '省份编码';
ALTER TABLE system_tenant ADD COLUMN city_code INT NULL COMMENT '城市编码';
ALTER TABLE system_tenant ADD COLUMN district_code INT NULL COMMENT '区县编码';
