-- 测试数据：AuthUserServiceTest
-- 使用唯一前缀 svc-user-test-* 和唯一UUID确保测试数据隔离

merge into "auth_user" ("id", "username", "tenant_id", "login_password", "supervisor_id", "dept_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('30000000-0000-0000-0000-000000000016', 'svc-user-test-1', 'svc-tenant-user-test-1', 'encrypted-pwd-1', '00000000-0000-0000-0000-000000000000', '30000000-0000-0000-0000-000000000020', 'from AuthUserServiceTest', true, false, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000017', 'svc-user-test-2', 'svc-tenant-user-test-1', 'encrypted-pwd-2', '30000000-0000-0000-0000-000000000016', '30000000-0000-0000-0000-000000000020', 'from AuthUserServiceTest', true, false, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000018', 'svc-user-test-3', 'svc-tenant-user-test-1', 'encrypted-pwd-3', '30000000-0000-0000-0000-000000000016', '30000000-0000-0000-0000-000000000021', 'from AuthUserServiceTest', true, false, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000019', 'svc-user-test-4', 'svc-tenant-user-test-2', 'encrypted-pwd-4', '00000000-0000-0000-0000-000000000000', null, 'from AuthUserServiceTest', false, false, 'system', '系统');

-- 创建测试用的部门数据（用于测试 getUsersByDeptId）
merge into "auth_dept" ("id", "name", "tenant_id", "dept_type_dict_code", "sort_num", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('30000000-0000-0000-0000-000000000020', 'svc-dept-user-test-1', 'svc-tenant-user-test-1', 'DEPT_TYPE_TEST', 1, 'from AuthUserServiceTest', true, false, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000021', 'svc-dept-user-test-2', 'svc-tenant-user-test-1', 'DEPT_TYPE_TEST', 2, 'from AuthUserServiceTest', true, false, 'system', '系统');

-- 创建测试用的角色数据（用于测试 getUsersByRoleCode）
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('30000000-0000-0000-0000-000000000022', 'svc-role-user-test-1', 'svc-role-user-test-1-name', 'svc-tenant-user-test-1', 'ams', 'from AuthUserServiceTest', true, false, 'system', '系统');

-- 创建角色-用户关系（用于测试 getUsersByRoleCode）
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name")
    values ('30000000-0000-0000-0000-000000000023', '30000000-0000-0000-0000-000000000022', '30000000-0000-0000-0000-000000000016', 'system', '系统'),
           ('30000000-0000-0000-0000-000000000024', '30000000-0000-0000-0000-000000000022', '30000000-0000-0000-0000-000000000017', 'system', '系统');
