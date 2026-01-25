-- 测试数据：AuthDeptUserServiceTest
-- 使用唯一前缀 svc-deptuser-test-* 和唯一UUID确保测试数据隔离

-- 创建测试用的用户
merge into "auth_user" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('6cd22b48-0000-0000-0000-000000000060', 'svc-user-deptus-test-1-xYvfu9vP', 'svc-tenan-deptu-test-1-xYvfu9vP', 'encrypted-pwd-1-xYvfu9vP', '00000000-0000-0000-0000-000000000000', 'from AuthDeptUserServiceTest', true, false, 'system', '系统'),
           ('6cd22b48-0000-0000-0000-000000000061', 'svc-user-deptus-test-2-xYvfu9vP', 'svc-tenan-deptu-test-1-xYvfu9vP', 'encrypted-pwd-2-xYvfu9vP', '00000000-0000-0000-0000-000000000000', 'from AuthDeptUserServiceTest', true, false, 'system', '系统'),
           ('6cd22b48-0000-0000-0000-000000000062', 'svc-user-deptus-test-3-xYvfu9vP', 'svc-tenan-deptu-test-1-xYvfu9vP', 'encrypted-pwd-3-xYvfu9vP', '00000000-0000-0000-0000-000000000000', 'from AuthDeptUserServiceTest', true, false, 'system', '系统');

-- 创建测试用的部门
merge into "auth_dept" ("id", "name", "tenant_id", "dept_type_dict_code", "sort_num", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('6cd22b48-0000-0000-0000-000000000063', 'svc-dept-deptus-test-1-xYvfu9vP', 'svc-tenan-deptu-test-1-xYvfu9vP', 'DEPT_TYPE_TEST', 1, 'from AuthDeptUserServiceTest', true, false, 'system', '系统'),
           ('6cd22b48-0000-0000-0000-000000000064', 'svc-dept-deptus-test-2-xYvfu9vP', 'svc-tenan-deptu-test-1-xYvfu9vP', 'DEPT_TYPE_TEST', 2, 'from AuthDeptUserServiceTest', true, false, 'system', '系统');

-- 创建已存在的部门-用户关系（用于测试exists和unbind）
merge into "auth_dept_user" ("id", "dept_id", "user_id", "dept_admin", "create_user_id", "create_user_name")
    values ('6cd22b48-0000-0000-0000-000000000065', '6cd22b48-0000-0000-0000-000000000063', '6cd22b48-0000-0000-0000-000000000060', true, 'system', '系统'),
           ('6cd22b48-0000-0000-0000-000000000066', '6cd22b48-0000-0000-0000-000000000063', '6cd22b48-0000-0000-0000-000000000061', false, 'system', '系统');
