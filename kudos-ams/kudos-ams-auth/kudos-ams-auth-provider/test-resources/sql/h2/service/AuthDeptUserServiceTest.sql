-- 测试数据：AuthDeptUserServiceTest
-- 使用唯一前缀 svc-deptuser-test-* 和唯一UUID确保测试数据隔离

-- 创建测试用的用户
merge into "auth_user" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('30000000-0000-0000-0000-000000000060', 'svc-user-deptuser-test-1', 'svc-tenant-deptuser-test-1', 'encrypted-pwd-1', '00000000-0000-0000-0000-000000000000', 'from AuthDeptUserServiceTest', true, false, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000061', 'svc-user-deptuser-test-2', 'svc-tenant-deptuser-test-1', 'encrypted-pwd-2', '00000000-0000-0000-0000-000000000000', 'from AuthDeptUserServiceTest', true, false, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000062', 'svc-user-deptuser-test-3', 'svc-tenant-deptuser-test-1', 'encrypted-pwd-3', '00000000-0000-0000-0000-000000000000', 'from AuthDeptUserServiceTest', true, false, 'system', '系统');

-- 创建测试用的部门
merge into "auth_dept" ("id", "name", "tenant_id", "dept_type_dict_code", "sort_num", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('30000000-0000-0000-0000-000000000063', 'svc-dept-deptuser-test-1', 'svc-tenant-deptuser-test-1', 'DEPT_TYPE_TEST', 1, 'from AuthDeptUserServiceTest', true, false, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000064', 'svc-dept-deptuser-test-2', 'svc-tenant-deptuser-test-1', 'DEPT_TYPE_TEST', 2, 'from AuthDeptUserServiceTest', true, false, 'system', '系统');

-- 创建已存在的部门-用户关系（用于测试exists和unbind）
merge into "auth_dept_user" ("id", "dept_id", "user_id", "dept_admin", "create_user_id", "create_user_name")
    values ('30000000-0000-0000-0000-000000000065', '30000000-0000-0000-0000-000000000063', '30000000-0000-0000-0000-000000000060', true, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000066', '30000000-0000-0000-0000-000000000063', '30000000-0000-0000-0000-000000000061', false, 'system', '系统');
