-- 新增区域编码字段，用于监管角色的数据权限区域过滤
-- 存储格式：数字编码路径，如"330000/330100/330106"
ALTER TABLE system_users ADD COLUMN area_code VARCHAR(50) NULL COMMENT '区域编码（数字编码路径），仅监管角色用户填写';
