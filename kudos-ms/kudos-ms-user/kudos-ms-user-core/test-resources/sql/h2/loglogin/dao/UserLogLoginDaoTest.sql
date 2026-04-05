-- user_log_login: 每条 id 唯一，使用 user-loglogin-dao-test-* 前缀隔离
merge into "user_log_login" ("id", "user_id", "username", "tenant_id", "login_time", "login_ip", "login_success", "create_user_id", "create_user_name") values
    ('588f6ca8-0000-0000-0000-000000000030', '588f6ca8-0000-0000-0000-000000000001', 'user-account-dao-test-1-rEj1639e', 'user-tenant-dao-test-1-rEj1639e', CURRENT_TIMESTAMP, 192168001001, true, 'system', '系统'),
    ('588f6ca8-0000-0000-0000-000000000031', '588f6ca8-0000-0000-0000-000000000002', 'user-account-dao-test-2-rEj1639e', 'user-tenant-dao-test-1-rEj1639e', CURRENT_TIMESTAMP, 192168001002, true, 'system', '系统'),
    ('588f6ca8-0000-0000-0000-000000000032', null, 'user-account-dao-test-fail-rEj1639e', 'user-tenant-dao-test-1-rEj1639e', CURRENT_TIMESTAMP, 192168001003, false, 'system', '系统');
