-- 测试数据：AuthRoleServiceTest
-- 使用唯一前缀 svc-role-test-* 和唯一UUID确保测试数据隔离

merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('249363d1-0000-0000-0000-000000000025', 'svc-role-test-1-bq0Y0mrl', 'svc-role-test-1-name-bq0Y0mrl', 'svc-tenant-role-test-1-bq0Y0mrl', 'ams', 'from AuthRoleServiceTest', true, false, 'system', '系统'),
           ('249363d1-0000-0000-0000-000000000026', 'svc-role-test-2-bq0Y0mrl', 'svc-role-test-2-name-bq0Y0mrl', 'svc-tenant-role-test-1-bq0Y0mrl', 'ams', 'from AuthRoleServiceTest', true, false, 'system', '系统'),
           ('249363d1-0000-0000-0000-000000000027', 'svc-role-test-3-bq0Y0mrl', 'svc-role-test-3-name-bq0Y0mrl', 'svc-tenant-role-test-1-bq0Y0mrl', 'svc-subsys-role-test-1-bq0Y0mrl', 'from AuthRoleServiceTest', true, false, 'system', '系统'),
           ('249363d1-0000-0000-0000-000000000028', 'svc-role-test-4-bq0Y0mrl', 'svc-role-test-4-name-bq0Y0mrl', 'svc-tenant-role-test-1-bq0Y0mrl', 'ams', 'from AuthRoleServiceTest', false, false, 'system', '系统'),
           ('249363d1-0000-0000-0000-000000000029', 'svc-role-test-5-bq0Y0mrl', 'svc-role-test-5-name-bq0Y0mrl', 'svc-tenant-role-test-2-bq0Y0mrl', 'ams', 'from AuthRoleServiceTest', true, false, 'system', '系统');

-- 创建测试用的角色数据（用于测试 getUsersByRoleCode）
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('249363d1-0000-0000-0000-000000000022', 'svc-role-user-test-1-249363d1', 'svc-rol-use-tes-1-name-249363d1', 'svc-tenant-user-test-1-249363d1', 'ams', 'from UserAccountServiceTest', true, false, 'system', '系统');

merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "org_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('249363d1-0000-0000-0000-000000000016', 'svc-user-test-1-249363d1', 'svc-tenant-user-test-1-249363d1', 'encrypted-pwd-1-249363d1', '00000000-0000-0000-0000-000000000000', '249363d1-0000-0000-0000-000000000020', 'from UserAccountServiceTest', true, false, 'system', '系统'),
    ('249363d1-0000-0000-0000-000000000017', 'svc-user-test-2-249363d1', 'svc-tenant-user-test-1-249363d1', 'encrypted-pwd-2-249363d1', '249363d1-0000-0000-0000-000000000016', '249363d1-0000-0000-0000-000000000020', 'from UserAccountServiceTest', true, false, 'system', '系统'),
    ('249363d1-0000-0000-0000-000000000018', 'svc-user-test-3-249363d1', 'svc-tenant-user-test-1-249363d1', 'encrypted-pwd-3-249363d1', '249363d1-0000-0000-0000-000000000016', '249363d1-0000-0000-0000-000000000021', 'from UserAccountServiceTest', true, false, 'system', '系统'),
    ('249363d1-0000-0000-0000-000000000019', 'svc-user-test-4-249363d1', 'svc-tenant-user-test-2-249363d1', 'encrypted-pwd-4-249363d1', '00000000-0000-0000-0000-000000000000', null, 'from UserAccountServiceTest', false, false, 'system', '系统');

-- 创建角色-用户关系（用于测试 getUsersByRoleCode）
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name")
    values ('249363d1-0000-0000-0000-000000000023', '249363d1-0000-0000-0000-000000000022', '249363d1-0000-0000-0000-000000000016', 'system', '系统'),
    ('249363d1-0000-0000-0000-000000000024', '249363d1-0000-0000-0000-000000000022', '249363d1-0000-0000-0000-000000000017', 'system', '系统');
