merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in") values
    ('20000000-0000-0000-0000-000000003675', 'svc-tenant-ts-test-1', null, null, 'from SysTenantSystemServiceTest', true, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in") values
    ('svc-system-ts-test-0_7901', 'svc-system-ts-test-1-name', null, false, 'from SysTenantSystemServiceTest', true, false),
    ('svc-subsys-ts-test-1_7901', 'svc-subsys-ts-test-1_7901-name', 'svc-system-ts-test-0_7901', true, 'from SysTenantSystemServiceTest', true, false);

merge into "sys_tenant_system" ("id", "tenant_id", "system_code") values
    ('20000000-0000-0000-0000-000000003675', '20000000-0000-0000-0000-000000003675', 'svc-subsys-ts-test-1_7901');
