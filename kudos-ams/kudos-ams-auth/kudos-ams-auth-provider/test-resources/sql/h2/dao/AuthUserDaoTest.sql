-- 测试数据：AuthUserDaoTest
-- 使用唯一前缀 auth-user-dao-test-* 和唯一UUID确保测试数据隔离

merge into "auth_user" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('c07f7328-0000-0000-0000-000000000001', 'auth-user-dao-test-1-Y6hpgGno', 'auth-tenant-dao-test-1-Y6hpgGno', 'encrypted-pwd-1-Y6hpgGno', '00000000-0000-0000-0000-000000000000', 'from AuthUserDaoTest', true, false, 'system', '系统'),
           ('c07f7328-0000-0000-0000-000000000002', 'auth-user-dao-test-2-Y6hpgGno', 'auth-tenant-dao-test-1-Y6hpgGno', 'encrypted-pwd-2-Y6hpgGno', 'c07f7328-0000-0000-0000-000000000001', 'from AuthUserDaoTest', true, false, 'system', '系统'),
           ('c07f7328-0000-0000-0000-000000000003', 'auth-user-dao-test-3-Y6hpgGno', 'auth-tenant-dao-test-2-Y6hpgGno', 'encrypted-pwd-3-Y6hpgGno', '00000000-0000-0000-0000-000000000000', 'from AuthUserDaoTest', false, false, 'system', '系统');