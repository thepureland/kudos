-- 测试数据：AuthUserDaoTest
-- 使用唯一前缀 auth-user-dao-test-* 和唯一UUID确保测试数据隔离

merge into "auth_user" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('50000000-0000-0000-0000-000000000001', 'auth-user-dao-test-1', 'auth-tenant-dao-test-1', 'encrypted-pwd-1', '00000000-0000-0000-0000-000000000000', 'from AuthUserDaoTest', true, false, 'system', '系统'),
           ('50000000-0000-0000-0000-000000000002', 'auth-user-dao-test-2', 'auth-tenant-dao-test-1', 'encrypted-pwd-2', '50000000-0000-0000-0000-000000000001', 'from AuthUserDaoTest', true, false, 'system', '系统'),
           ('50000000-0000-0000-0000-000000000003', 'auth-user-dao-test-3', 'auth-tenant-dao-test-2', 'encrypted-pwd-3', '00000000-0000-0000-0000-000000000000', 'from AuthUserDaoTest', false, false, 'system', '系统');