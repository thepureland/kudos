-- 测试数据：AuthRoleUserServiceTest
-- 使用唯一前缀 svc-roleuser-test-* 和唯一UUID确保测试数据隔离

-- 创建测试用的用户
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('7817d37f-0000-0000-0000-000000000040', 'svc-user-roleus-test-1-BuHDHRZc', 'svc-tenan-roleu-test-1-BuHDHRZc', 'encrypted-pwd-1-BuHDHRZc', '00000000-0000-0000-0000-000000000000', 'from AuthRoleUserServiceTest', true, false, 'system', '系统'),
           ('7817d37f-0000-0000-0000-000000000041', 'svc-user-roleus-test-2-BuHDHRZc', 'svc-tenan-roleu-test-1-BuHDHRZc', 'encrypted-pwd-2-BuHDHRZc', '00000000-0000-0000-0000-000000000000', 'from AuthRoleUserServiceTest', true, false, 'system', '系统'),
           ('7817d37f-0000-0000-0000-000000000042', 'svc-user-roleus-test-3-BuHDHRZc', 'svc-tenan-roleu-test-1-BuHDHRZc', 'encrypted-pwd-3-BuHDHRZc', '00000000-0000-0000-0000-000000000000', 'from AuthRoleUserServiceTest', true, false, 'system', '系统');

-- 创建测试用的角色
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('7817d37f-0000-0000-0000-000000000043', 'svc-role-roleus-test-1-BuHDHRZc', 'svc-rol-rol-tes-1-name-BuHDHRZc', 'svc-tenan-roleu-test-1-BuHDHRZc', 'ams', 'from AuthRoleUserServiceTest', true, false, 'system', '系统'),
           ('7817d37f-0000-0000-0000-000000000044', 'svc-role-roleus-test-2-BuHDHRZc', 'svc-rol-rol-tes-2-name-BuHDHRZc', 'svc-tenan-roleu-test-1-BuHDHRZc', 'ams', 'from AuthRoleUserServiceTest', true, false, 'system', '系统');

-- 创建已存在的角色-用户关系（用于测试exists和unbind）
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name")
    values ('7817d37f-0000-0000-0000-000000000045', '7817d37f-0000-0000-0000-000000000043', '7817d37f-0000-0000-0000-000000000040', 'system', '系统'),
           ('7817d37f-0000-0000-0000-000000000046', '7817d37f-0000-0000-0000-000000000043', '7817d37f-0000-0000-0000-000000000041', 'system', '系统');
