-- 测试数据：AuthGroupDaoTest
-- 使用唯一前缀 auth-group-dao-test-* 和唯一UUID确保测试数据隔离

merge into "auth_group" ("id", "group_code", "group_name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('9a4a2d1b-0000-0000-0000-000000000020', 'auth-group-dao-test-1-8GkL2Mxy', 'auth-group-dao-test-1-name-8GkL2Mxy', 'auth-tenant-dao-test-1-8GkL2Mxy', 'ams', 'from AuthGroupDaoTest', true, false, 'system', '系统'),
           ('9a4a2d1b-0000-0000-0000-000000000021', 'auth-group-dao-test-2-8GkL2Mxy', 'auth-group-dao-test-2-name-8GkL2Mxy', 'auth-tenant-dao-test-1-8GkL2Mxy', 'ams', 'from AuthGroupDaoTest', true, false, 'system', '系统'),
           ('9a4a2d1b-0000-0000-0000-000000000022', 'auth-group-dao-test-3-8GkL2Mxy', 'auth-group-dao-test-3-name-8GkL2Mxy', 'auth-tenant-dao-test-2-8GkL2Mxy', 'ams', 'from AuthGroupDaoTest', false, false, 'system', '系统');
