merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000032', 'svc-tenant-domain-test-1', null, null, 'from SysDomainServiceTest', true, false);

merge into "sys_portal" ("code", "name", "remark", "active", "built_in")
    values ('svc-portal-domain-test-1', 'svc-portal-domain-test-1-name', 'from SysDomainServiceTest', true, false);

merge into "sys_sub_system" ("code", "name", "portal_code", "remark", "active", "built_in")
    values ('svc-subsys-domain-test-1', 'svc-subsys-domain-test-1-name', 'svc-portal-domain-test-1', 'from SysDomainServiceTest', true, false);

merge into "sys_domain" ("id", "domain", "sub_system_code", "portal_code", "tenant_id", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000032', 'svc-domain-test-1.com', 'svc-subsys-domain-test-1', 'svc-portal-domain-test-1', '20000000-0000-0000-0000-000000000032', 'from SysDomainServiceTest', true, false);
