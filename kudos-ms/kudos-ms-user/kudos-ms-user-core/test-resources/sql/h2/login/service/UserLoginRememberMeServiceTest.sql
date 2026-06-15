-- user_account: 供 user_login_remember_me.user_id 外键引用
merge into "user_account" ("id", "username", "tenant_id", "login_password", "display_name", "supervisor_id", "remark", "active", "built_in") values
    ('33330000-0000-0000-0000-000000000001', 'remember-user-1', 'svc-rt-tenant-seed', 'password', 'Remember Svc User1', '00000000-0000-0000-0000-000000000000', '测试用户', true, false);

-- user_login_remember_me: 每条 id 唯一，供 UserLoginRememberMeService 用例使用
-- 注意: user_id / tenant_id / token 均为 NOT NULL(见 V1.0.0.22 DDL)，必须写入。
merge into "user_login_remember_me" ("id", "user_id", "username", "tenant_id", "token", "last_used") values
    ('33333333-0000-0000-0000-000000000001', '33330000-0000-0000-0000-000000000001', 'remember-user-1', 'svc-rt-tenant-seed', 'token-1', CURRENT_TIMESTAMP);
