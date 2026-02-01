merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000024', 'svc-tenant-test-1', null, null, 'from SysTenantServiceTest', true, false);

merge into "sys_system" ("code", "name", "remark", "active", "built_in")
    values ('svc-system-tenant-test-1', 'svc-system-tenant-test-1-name', 'from SysTenantServiceTest', true, false);

merge into "sys_sub_system" ("code", "name", "system_code", "remark", "active", "built_in")
    values ('svc-subsys-tenant-test-1', 'svc-subsys-tenant-test-1-name', 'svc-system-tenant-test-1', 'from SysTenantServiceTest', true, false);

merge into "sys_tenant_sub_system" ("id", "tenant_id", "sub_system_code", "system_code")
    values ('20000000-0000-0000-0000-000000000024', '20000000-0000-0000-0000-000000000024', 'svc-subsys-tenant-test-1', 'svc-system-tenant-test-1');
