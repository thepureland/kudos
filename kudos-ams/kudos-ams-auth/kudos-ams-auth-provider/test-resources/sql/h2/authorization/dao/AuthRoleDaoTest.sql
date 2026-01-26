-- 测试数据：AuthRoleDaoTest
-- 使用唯一前缀 auth-role-dao-test-* 和唯一UUID确保测试数据隔离

merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('7e4a2d1b-0000-0000-0000-000000000020', 'auth-role-dao-test-1-DqEGww0j', 'auth-rol-dao-tes-1-name-DqEGww0j', 'auth-tenant-dao-test-1-DqEGww0j', 'ams', 'from AuthRoleDaoTest', true, false, 'system', '系统'),
           ('7e4a2d1b-0000-0000-0000-000000000021', 'auth-role-dao-test-2-DqEGww0j', 'auth-rol-dao-tes-2-name-DqEGww0j', 'auth-tenant-dao-test-1-DqEGww0j', 'ams', 'from AuthRoleDaoTest', true, false, 'system', '系统'),
           ('7e4a2d1b-0000-0000-0000-000000000022', 'auth-role-dao-test-3-DqEGww0j', 'auth-rol-dao-tes-3-name-DqEGww0j', 'auth-tenant-dao-test-2-DqEGww0j', 'ams', 'from AuthRoleDaoTest', false, false, 'system', '系统');