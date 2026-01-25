-- 测试数据：AuthDeptServiceTest
-- 使用唯一前缀 svc-dept-test-* 和唯一UUID确保测试数据隔离

merge into "auth_dept" ("id", "name", "short_name", "tenant_id", "parent_id", "dept_type_dict_code", "sort_num", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('8b4df430-0000-0000-0000-000000000030', 'svc-dept-test-root-1-HuAyup4R', 'svc-dept-test-root-1-HuAyup4R', 'svc-tenant-dept-test-1-HuAyup4R', null, 'DEPT_TYPE_TEST', 1, 'from AuthDeptServiceTest', true, false, 'system', '系统'),
           ('8b4df430-0000-0000-0000-000000000031', 'svc-dept-test-child-1-HuAyup4R', 'svc-dept-test-child-1-HuAyup4R', 'svc-tenant-dept-test-1-HuAyup4R', '8b4df430-0000-0000-0000-000000000030', 'DEPT_TYPE_TEST', 11, 'from AuthDeptServiceTest', true, false, 'system', '系统'),
           ('8b4df430-0000-0000-0000-000000000032', 'svc-dept-test-child-2-HuAyup4R', 'svc-dept-test-child-2-HuAyup4R', 'svc-tenant-dept-test-1-HuAyup4R', '8b4df430-0000-0000-0000-000000000030', 'DEPT_TYPE_TEST', 12, 'from AuthDeptServiceTest', true, false, 'system', '系统'),
           ('8b4df430-0000-0000-0000-000000000033', 'svc-dept-test-grandchild-1-HuAyup4R', 'svc-dept-test-grch-1-HuAyup4R', 'svc-tenant-dept-test-1-HuAyup4R', '8b4df430-0000-0000-0000-000000000031', 'DEPT_TYPE_TEST', 111, 'from AuthDeptServiceTest', true, false, 'system', '系统'),
           ('8b4df430-0000-0000-0000-000000000034', 'svc-dept-test-root-2-HuAyup4R', 'svc-dept-test-root-2-HuAyup4R', 'svc-tenant-dept-test-1-HuAyup4R', null, 'DEPT_TYPE_TEST', 2, 'from AuthDeptServiceTest', true, false, 'system', '系统'),
           ('8b4df430-0000-0000-0000-000000000035', 'svc-dept-test-inactive', 'svc-dept-test-inactive', 'svc-tenant-dept-test-1-HuAyup4R', null, 'DEPT_TYPE_TEST', 3, 'from AuthDeptServiceTest', false, false, 'system', '系统'),
           ('8b4df430-0000-0000-0000-000000000036', 'svc-dept-test-tenant2-HuAyup4R', 'svc-dept-test-tenant2-HuAyup4R', 'svc-tenant-dept-test-2-HuAyup4R', null, 'DEPT_TYPE_TEST', 1, 'from AuthDeptServiceTest', true, false, 'system', '系统');
