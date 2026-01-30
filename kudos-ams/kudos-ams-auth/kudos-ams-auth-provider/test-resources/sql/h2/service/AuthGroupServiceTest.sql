-- 测试数据：AuthGroupServiceTest

merge into "auth_group" ("id", "group_code", "group_name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('9a4a2d1b-0000-0000-0000-000000000030', 'svc-group-test-1-r9KxqP1a', 'svc-group-test-1-name-r9KxqP1a', 'svc-tenant-group-test-1-r9KxqP1a', 'ams', 'from AuthGroupServiceTest', true, false, 'system', '系统'),
           ('9a4a2d1b-0000-0000-0000-000000000031', 'svc-group-test-2-r9KxqP1a', 'svc-group-test-2-name-r9KxqP1a', 'svc-tenant-group-test-1-r9KxqP1a', 'ams', 'from AuthGroupServiceTest', true, false, 'system', '系统');
