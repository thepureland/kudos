-- 测试数据：AuthDeptUserDaoTest
-- 使用唯一前缀 auth-deptuser-dao-test-* 和唯一UUID确保测试数据隔离

-- 创建测试用的用户
merge into "auth_user" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('50000000-0000-0000-0000-000000000040', 'auth-user-deptuser-dao-test-1', 'auth-tenant-deptuser-dao-test-1', 'encrypted-pwd-1', '00000000-0000-0000-0000-000000000000', 'from AuthDeptUserDaoTest', true, false, 'system', '系统'),
           ('50000000-0000-0000-0000-000000000041', 'auth-user-deptuser-dao-test-2', 'auth-tenant-deptuser-dao-test-1', 'encrypted-pwd-2', '00000000-0000-0000-0000-000000000000', 'from AuthDeptUserDaoTest', true, false, 'system', '系统');

-- 创建测试用的部门
merge into "auth_dept" ("id", "name", "tenant_id", "dept_type_dict_code", "sort_num", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('50000000-0000-0000-0000-000000000042', 'auth-dept-deptuser-dao-test-1', 'auth-tenant-deptuser-dao-test-1', 'DEPT_TYPE_TEST', 1, 'from AuthDeptUserDaoTest', true, false, 'system', '系统'),
           ('50000000-0000-0000-0000-000000000043', 'auth-dept-deptuser-dao-test-2', 'auth-tenant-deptuser-dao-test-1', 'DEPT_TYPE_TEST', 2, 'from AuthDeptUserDaoTest', true, false, 'system', '系统');

-- 创建已存在的部门-用户关系（用于测试exists）
merge into "auth_dept_user" ("id", "dept_id", "user_id", "dept_admin", "create_user_id", "create_user_name")
    values ('50000000-0000-0000-0000-000000000044', '50000000-0000-0000-0000-000000000042', '50000000-0000-0000-0000-000000000040', true, 'system', '系统'),
           ('50000000-0000-0000-0000-000000000045', '50000000-0000-0000-0000-000000000042', '50000000-0000-0000-0000-000000000041', false, 'system', '系统');