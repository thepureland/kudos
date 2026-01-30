-- 测试数据：AuthGroupUserDaoTest
-- 使用唯一前缀 auth-group-user-dao-test-* 和唯一UUID确保测试数据隔离

-- 创建测试用的用户
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('a1b2c3d4-0000-0000-0000-000000000070', 'auth-group-user-dao-test-1-0xQpE1Zf', 'auth-tenant-gpu-dao-test-1-0xQpE1Zf', 'encrypted-pwd-1-0xQpE1Zf', '00000000-0000-0000-0000-000000000000', 'from AuthGroupUserDaoTest', true, false, 'system', '系统'),
           ('a1b2c3d4-0000-0000-0000-000000000071', 'auth-group-user-dao-test-2-0xQpE1Zf', 'auth-tenant-gpu-dao-test-1-0xQpE1Zf', 'encrypted-pwd-2-0xQpE1Zf', '00000000-0000-0000-0000-000000000000', 'from AuthGroupUserDaoTest', true, false, 'system', '系统');

-- 创建测试用的组
merge into "auth_group" ("id", "group_code", "group_name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('a1b2c3d4-0000-0000-0000-000000000072', 'auth-group-user-dao-test-1-0xQpE1Zf', 'auth-group-user-dao-test-1-name-0xQpE1Zf', 'auth-tenant-gpu-dao-test-1-0xQpE1Zf', 'ams', 'from AuthGroupUserDaoTest', true, false, 'system', '系统'),
           ('a1b2c3d4-0000-0000-0000-000000000073', 'auth-group-user-dao-test-2-0xQpE1Zf', 'auth-group-user-dao-test-2-name-0xQpE1Zf', 'auth-tenant-gpu-dao-test-1-0xQpE1Zf', 'ams', 'from AuthGroupUserDaoTest', true, false, 'system', '系统');

-- 创建已存在的组-用户关系（用于测试exists和查询）
merge into "auth_group_user" ("id", "group_id", "user_id", "create_user_id", "create_user_name")
    values ('a1b2c3d4-0000-0000-0000-000000000074', 'a1b2c3d4-0000-0000-0000-000000000072', 'a1b2c3d4-0000-0000-0000-000000000070', 'system', '系统'),
           ('a1b2c3d4-0000-0000-0000-000000000075', 'a1b2c3d4-0000-0000-0000-000000000072', 'a1b2c3d4-0000-0000-0000-000000000071', 'system', '系统');
