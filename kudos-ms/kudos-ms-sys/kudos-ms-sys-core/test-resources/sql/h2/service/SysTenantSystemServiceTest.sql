merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000025', 'svc-tenant-ts-test-1', null, null, 'from SysTenantSystemServiceTest', true, false);

merge into "sys_system" ("code", "name", "remark", "active", "built_in")
    values ('svc-system-ts-test-1', 'svc-system-ts-test-1-name', 'from SysTenantSystemServiceTest', true, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in")
    values ('svc-subsys-ts-test-1', 'svc-subsys-ts-test-1-name', 'svc-system-ts-test-1', true, 'from SysTenantSystemServiceTest', true, false);

merge into "sys_tenant_system" ("id", "tenant_id", "system_code")
    values ('20000000-0000-0000-0000-000000000025', '20000000-0000-0000-0000-000000000025', 'svc-subsys-ts-test-1');
