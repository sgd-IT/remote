-- 登录连接超时策略：将刷新令牌有效期从 600s(10分钟) 调整为 1800s(30分钟)
-- 配合滑动续期机制，用户无交互 30 分钟后自动退出登录
UPDATE system_oauth2_client
SET refresh_token_validity_seconds = 1800
WHERE client_id = 'default'
  AND refresh_token_validity_seconds = 600;
