-- 测试数据：AuthRoleUserServiceTest
-- 使用唯一前缀 svc-roleuser-test-* 和唯一UUID确保测试数据隔离

-- 创建测试用的用户
merge into "auth_user" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('30000000-0000-0000-0000-000000000040', 'svc-user-roleuser-test-1', 'svc-tenant-roleuser-test-1', 'encrypted-pwd-1', '00000000-0000-0000-0000-000000000000', 'from AuthRoleUserServiceTest', true, false, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000041', 'svc-user-roleuser-test-2', 'svc-tenant-roleuser-test-1', 'encrypted-pwd-2', '00000000-0000-0000-0000-000000000000', 'from AuthRoleUserServiceTest', true, false, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000042', 'svc-user-roleuser-test-3', 'svc-tenant-roleuser-test-1', 'encrypted-pwd-3', '00000000-0000-0000-0000-000000000000', 'from AuthRoleUserServiceTest', true, false, 'system', '系统');

-- 创建测试用的角色
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('30000000-0000-0000-0000-000000000043', 'svc-role-roleuser-test-1', 'svc-role-roleuser-test-1-name', 'svc-tenant-roleuser-test-1', 'ams', 'from AuthRoleUserServiceTest', true, false, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000044', 'svc-role-roleuser-test-2', 'svc-role-roleuser-test-2-name', 'svc-tenant-roleuser-test-1', 'ams', 'from AuthRoleUserServiceTest', true, false, 'system', '系统');

-- 创建已存在的角色-用户关系（用于测试exists和unbind）
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name")
    values ('30000000-0000-0000-0000-000000000045', '30000000-0000-0000-0000-000000000043', '30000000-0000-0000-0000-000000000040', 'system', '系统'),
           ('30000000-0000-0000-0000-000000000046', '30000000-0000-0000-0000-000000000043', '30000000-0000-0000-0000-000000000041', 'system', '系统');
