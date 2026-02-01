merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000025', 'svc-tenant-ts-test-1', null, null, 'from SysTenantSubSystemServiceTest', true, false);

merge into "sys_system" ("code", "name", "remark", "active", "built_in")
    values ('svc-system-ts-test-1', 'svc-system-ts-test-1-name', 'from SysTenantSubSystemServiceTest', true, false);

merge into "sys_sub_system" ("code", "name", "system_code", "remark", "active", "built_in")
    values ('svc-subsys-ts-test-1', 'svc-subsys-ts-test-1-name', 'svc-system-ts-test-1', 'from SysTenantSubSystemServiceTest', true, false);

merge into "sys_tenant_sub_system" ("id", "tenant_id", "sub_system_code", "system_code")
    values ('20000000-0000-0000-0000-000000000025', '20000000-0000-0000-0000-000000000025', 'svc-subsys-ts-test-1', 'svc-system-ts-test-1');
