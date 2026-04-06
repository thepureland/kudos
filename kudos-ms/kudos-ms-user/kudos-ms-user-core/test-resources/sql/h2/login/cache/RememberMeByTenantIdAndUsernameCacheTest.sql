-- user_account: 每条 id 唯一，(tenant_id, username) 供记住我关联
merge into "user_account" ("id", "username", "tenant_id", "login_password", "display_name", "supervisor_id", "remark", "active", "built_in") values
    ('7c1a0000-0000-0000-0000-000000000001', 'remember_user1', 'tenant-remember-1', 'password', 'Remember User1', '00000000-0000-0000-0000-000000000000', '测试用户1', true, false),
    ('7c1a0000-0000-0000-0000-000000000002', 'remember_user2', 'tenant-remember-1', 'password', 'Remember User2', '00000000-0000-0000-0000-000000000000', '测试用户2', true, false),
    ('7c1a0000-0000-0000-0000-000000000003', 'remember_user_delete', 'tenant-remember-1', 'password', 'Remember User3', '00000000-0000-0000-0000-000000000000', '测试用户3', true, false),
    ('7c1a0000-0000-0000-0000-000000000004', 'remember_user1', 'tenant-remember-2', 'password', 'Remember User4', '00000000-0000-0000-0000-000000000000', '测试用户4', true, false);

-- user_login_remember_me: 每条 id 唯一，(tenant_id, username) 供缓存 key 使用
merge into "user_login_remember_me" ("id", "user_id", "username", "tenant_id", "token", "last_used") values
    ('8c1a0000-0000-0000-0000-000000000001', '7c1a0000-0000-0000-0000-000000000001', 'remember_user1', 'tenant-remember-1', 'token-1', CURRENT_TIMESTAMP),
    ('8c1a0000-0000-0000-0000-000000000002', '7c1a0000-0000-0000-0000-000000000002', 'remember_user2', 'tenant-remember-1', 'token-2', CURRENT_TIMESTAMP),
    ('8c1a0000-0000-0000-0000-000000000003', '7c1a0000-0000-0000-0000-000000000003', 'remember_user_delete', 'tenant-remember-1', 'token-del', CURRENT_TIMESTAMP),
    ('8c1a0000-0000-0000-0000-000000000004', '7c1a0000-0000-0000-0000-000000000004', 'remember_user1', 'tenant-remember-2', 'token-3', CURRENT_TIMESTAMP);
