merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000024', 'svc-tenant-test-1', null, null, 'from SysTenantServiceTest', true, false);

merge into "sys_portal" ("code", "name", "remark", "active", "built_in")
    values ('svc-portal-tenant-test-1', 'svc-portal-tenant-test-1-name', 'from SysTenantServiceTest', true, false);

merge into "sys_sub_system" ("code", "name", "portal_code", "remark", "active", "built_in")
    values ('svc-subsys-tenant-test-1', 'svc-subsys-tenant-test-1-name', 'svc-portal-tenant-test-1', 'from SysTenantServiceTest', true, false);

merge into "sys_tenant_sub_system" ("id", "tenant_id", "sub_system_code", "portal_code")
    values ('20000000-0000-0000-0000-000000000024', '20000000-0000-0000-0000-000000000024', 'svc-subsys-tenant-test-1', 'svc-portal-tenant-test-1');
