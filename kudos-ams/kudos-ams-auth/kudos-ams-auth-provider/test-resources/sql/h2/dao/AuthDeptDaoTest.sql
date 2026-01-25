-- 测试数据：AuthDeptDaoTest
-- 使用唯一前缀 auth-dept-dao-test-* 和唯一UUID确保测试数据隔离

merge into "auth_dept" ("id", "name", "tenant_id", "dept_type_dict_code", "sort_num", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('669d8a45-0000-0000-0000-000000000010', 'auth-dept-dao-test-1-w2nwgBMU', 'auth-tenant-dao-test-1-w2nwgBMU', 'DEPT_TYPE_TEST', 1, 'from AuthDeptDaoTest', true, false, 'system', '系统'),
           ('669d8a45-0000-0000-0000-000000000011', 'auth-dept-dao-test-2-w2nwgBMU', 'auth-tenant-dao-test-1-w2nwgBMU', 'DEPT_TYPE_TEST', 2, 'from AuthDeptDaoTest', true, false, 'system', '系统'),
           ('669d8a45-0000-0000-0000-000000000012', 'auth-dept-dao-test-3-w2nwgBMU', 'auth-tenant-dao-test-2-w2nwgBMU', 'DEPT_TYPE_TEST', 3, 'from AuthDeptDaoTest', false, false, 'system', '系统');