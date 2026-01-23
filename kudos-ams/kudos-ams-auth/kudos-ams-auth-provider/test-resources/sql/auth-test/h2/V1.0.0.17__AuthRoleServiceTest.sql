-- 测试数据：AuthRoleServiceTest
-- 使用唯一前缀 svc-role-test-* 和唯一UUID确保测试数据隔离

merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('30000000-0000-0000-0000-000000000025', 'svc-role-test-1', 'svc-role-test-1-name', 'svc-tenant-role-test-1', 'ams', 'from AuthRoleServiceTest', true, false, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000026', 'svc-role-test-2', 'svc-role-test-2-name', 'svc-tenant-role-test-1', 'ams', 'from AuthRoleServiceTest', true, false, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000027', 'svc-role-test-3', 'svc-role-test-3-name', 'svc-tenant-role-test-1', 'svc-subsys-role-test-1', 'from AuthRoleServiceTest', true, false, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000028', 'svc-role-test-4', 'svc-role-test-4-name', 'svc-tenant-role-test-1', 'ams', 'from AuthRoleServiceTest', false, false, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000029', 'svc-role-test-5', 'svc-role-test-5-name', 'svc-tenant-role-test-2', 'ams', 'from AuthRoleServiceTest', true, false, 'system', '系统');
