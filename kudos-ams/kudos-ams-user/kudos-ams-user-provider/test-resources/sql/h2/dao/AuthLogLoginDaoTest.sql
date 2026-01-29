-- 测试数据：AuthLogLoginDaoTest
-- 使用唯一前缀 auth-loglogin-dao-test-* 和唯一UUID确保测试数据隔离

merge into "auth_log_login" ("id", "user_id", "username", "tenant_id", "login_time", "login_ip", "login_success", "create_user_id", "create_user_name")
    values ('588f6ca8-0000-0000-0000-000000000030', '588f6ca8-0000-0000-0000-000000000001', 'auth-user-dao-test-1-rEj1639e', 'auth-tenant-dao-test-1-rEj1639e', CURRENT_TIMESTAMP, 192168001001, true, 'system', '系统'),
           ('588f6ca8-0000-0000-0000-000000000031', '588f6ca8-0000-0000-0000-000000000002', 'auth-user-dao-test-2-rEj1639e', 'auth-tenant-dao-test-1-rEj1639e', CURRENT_TIMESTAMP, 192168001002, true, 'system', '系统'),
           ('588f6ca8-0000-0000-0000-000000000032', null, 'auth-user-dao-test-fail-rEj1639e', 'auth-tenant-dao-test-1-rEj1639e', CURRENT_TIMESTAMP, 192168001003, false, 'system', '系统');