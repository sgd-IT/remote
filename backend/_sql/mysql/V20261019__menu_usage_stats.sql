-- 使用统计页面菜单：挂在「API 日志」目录（id=1083）下，仅超级管理员可见
-- 超管靠角色直接拥有全部菜单，无需额外插 system_role_menu；其他角色既看不到入口，后端接口也会 403
-- 按 parent_id + path 判重，重复执行不会插出第二条
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, updater)
SELECT '使用统计', '', 2, 3, 1083, 'usage-stats', 'ep:data-analysis', 'infra/apiAccessLog/usageStats', 'InfraApiAccessLogUsageStats', 0, b'1', b'1', b'1', 'admin', 'admin'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu WHERE parent_id = 1083 AND path = 'usage-stats'
);
