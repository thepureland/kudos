merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in") values
    ('20000000-0000-0000-0000-000000006144', 'svc-tenant-test-1', null, null, 'from SysTenantServiceTest', true, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in") values
    ('svc-system-tenant-test-0_2492', 'svc-system-tenant-test-1-name', null, false, 'from SysTenantServiceTest', true, false),
    ('svc-subsys-tenant-test-1_2492', 'svc-subsys-tenant-test-1_2492-name', 'svc-system-tenant-test-0_2492', true, 'from SysTenantServiceTest', true, false);

merge into "sys_tenant_system" ("id", "tenant_id", "system_code") values
    ('20000000-0000-0000-0000-000000006144', '20000000-0000-0000-0000-000000006144', 'svc-subsys-tenant-test-1_2492');
