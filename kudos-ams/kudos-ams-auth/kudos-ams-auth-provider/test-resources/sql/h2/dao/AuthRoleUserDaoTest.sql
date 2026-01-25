-- 测试数据：AuthRoleUserDaoTest
-- 使用唯一前缀 auth-roleuser-dao-test-* 和唯一UUID确保测试数据隔离

-- 创建测试用的用户
merge into "auth_user" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('50000000-0000-0000-0000-000000000050', 'auth-user-roleuser-dao-test-1', 'auth-tenant-roleuser-dao-test-1', 'encrypted-pwd-1', '00000000-0000-0000-0000-000000000000', 'from AuthRoleUserDaoTest', true, false, 'system', '系统'),
           ('50000000-0000-0000-0000-000000000051', 'auth-user-roleuser-dao-test-2', 'auth-tenant-roleuser-dao-test-1', 'encrypted-pwd-2', '00000000-0000-0000-0000-000000000000', 'from AuthRoleUserDaoTest', true, false, 'system', '系统');

-- 创建测试用的角色
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('50000000-0000-0000-0000-000000000052', 'auth-role-roleuser-dao-test-1', 'auth-role-roleuser-dao-test-1-name', 'auth-tenant-roleuser-dao-test-1', 'ams', 'from AuthRoleUserDaoTest', true, false, 'system', '系统'),
           ('50000000-0000-0000-0000-000000000053', 'auth-role-roleuser-dao-test-2', 'auth-role-roleuser-dao-test-2-name', 'auth-tenant-roleuser-dao-test-1', 'ams', 'from AuthRoleUserDaoTest', true, false, 'system', '系统');

-- 创建已存在的角色-用户关系（用于测试exists）
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name")
    values ('50000000-0000-0000-0000-000000000054', '50000000-0000-0000-0000-000000000052', '50000000-0000-0000-0000-000000000050', 'system', '系统'),
           ('50000000-0000-0000-0000-000000000055', '50000000-0000-0000-0000-000000000052', '50000000-0000-0000-0000-000000000051', 'system', '系统');