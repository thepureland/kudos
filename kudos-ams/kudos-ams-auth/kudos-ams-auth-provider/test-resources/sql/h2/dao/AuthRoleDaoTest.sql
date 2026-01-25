-- 测试数据：AuthRoleDaoTest
-- 使用唯一前缀 auth-role-dao-test-* 和唯一UUID确保测试数据隔离

merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('50000000-0000-0000-0000-000000000020', 'auth-role-dao-test-1', 'auth-role-dao-test-1-name', 'auth-tenant-dao-test-1', 'ams', 'from AuthRoleDaoTest', true, false, 'system', '系统'),
           ('50000000-0000-0000-0000-000000000021', 'auth-role-dao-test-2', 'auth-role-dao-test-2-name', 'auth-tenant-dao-test-1', 'ams', 'from AuthRoleDaoTest', true, false, 'system', '系统'),
           ('50000000-0000-0000-0000-000000000022', 'auth-role-dao-test-3', 'auth-role-dao-test-3-name', 'auth-tenant-dao-test-2', 'ams', 'from AuthRoleDaoTest', false, false, 'system', '系统');