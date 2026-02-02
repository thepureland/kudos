merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000039', 'svc-tenant-tl-test-1', null, null, 'from SysTenantLocaleServiceTest', true, false);

merge into "sys_tenant_locale" ("id", "tenant_id", "locale_code")
    values ('20000000-0000-0000-0000-000000000039', '20000000-0000-0000-0000-000000000039', 'zh_CN');
