merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000025', 'svc-tenant-ts-test-1', null, null, 'from SysTenantSubSystemServiceTest', true, false);

merge into "sys_portal" ("code", "name", "remark", "active", "built_in")
    values ('svc-portal-ts-test-1', 'svc-portal-ts-test-1-name', 'from SysTenantSubSystemServiceTest', true, false);

merge into "sys_sub_system" ("code", "name", "portal_code", "remark", "active", "built_in")
    values ('svc-subsys-ts-test-1', 'svc-subsys-ts-test-1-name', 'svc-portal-ts-test-1', 'from SysTenantSubSystemServiceTest', true, false);

merge into "sys_tenant_sub_system" ("id", "tenant_id", "sub_system_code", "portal_code")
    values ('20000000-0000-0000-0000-000000000025', '20000000-0000-0000-0000-000000000025', 'svc-subsys-ts-test-1', 'svc-portal-ts-test-1');
