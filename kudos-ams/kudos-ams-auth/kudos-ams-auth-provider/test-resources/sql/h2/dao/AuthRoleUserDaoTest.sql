-- 测试数据：AuthRoleUserDaoTest
-- 使用唯一前缀 auth-roleuser-dao-test-* 和唯一UUID确保测试数据隔离

-- 创建测试用的用户
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('42d84639-0000-0000-0000-000000000050', 'user-acc-rol-dao-tes-1-kRWTnffD', 'auth-tena-rol-dao-tes-1-kRWTnffD', 'encrypted-pwd-1-kRWTnffD', '00000000-0000-0000-0000-000000000000', 'from AuthRoleUserDaoTest', true, false, 'system', '系统'),
           ('42d84639-0000-0000-0000-000000000051', 'user-acc-rol-dao-tes-2-kRWTnffD', 'auth-tena-rol-dao-tes-1-kRWTnffD', 'encrypted-pwd-2-kRWTnffD', '00000000-0000-0000-0000-000000000000', 'from AuthRoleUserDaoTest', true, false, 'system', '系统');

-- 创建测试用的角色
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('42d84639-0000-0000-0000-000000000052', 'auth-role-rol-dao-tes-1-kRWTnffD', 'auth-rol-ro-da-te-1-nam-kRWTnffD', 'auth-tena-rol-dao-tes-1-kRWTnffD', 'ams', 'from AuthRoleUserDaoTest', true, false, 'system', '系统'),
           ('42d84639-0000-0000-0000-000000000053', 'auth-role-rol-dao-tes-2-kRWTnffD', 'auth-rol-ro-da-te-2-nam-kRWTnffD', 'auth-tena-rol-dao-tes-1-kRWTnffD', 'ams', 'from AuthRoleUserDaoTest', true, false, 'system', '系统');

-- 创建已存在的角色-用户关系（用于测试exists）
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name")
    values ('42d84639-0000-0000-0000-000000000054', '42d84639-0000-0000-0000-000000000052', '42d84639-0000-0000-0000-000000000050', 'system', '系统'),
           ('42d84639-0000-0000-0000-000000000055', '42d84639-0000-0000-0000-000000000052', '42d84639-0000-0000-0000-000000000051', 'system', '系统');