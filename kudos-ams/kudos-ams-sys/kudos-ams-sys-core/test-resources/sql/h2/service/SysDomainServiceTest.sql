merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000032', 'svc-tenant-domain-test-1', null, null, 'from SysDomainServiceTest', true, false);

merge into "sys_system" ("code", "name", "remark", "active", "built_in")
    values ('svc-system-domain-test-1', 'svc-system-domain-test-1-name', 'from SysDomainServiceTest', true, false);

merge into "sys_sub_system" ("code", "name", "system_code", "remark", "active", "built_in")
    values ('svc-subsys-domain-test-1', 'svc-subsys-domain-test-1-name', 'svc-system-domain-test-1', 'from SysDomainServiceTest', true, false);

merge into "sys_domain" ("id", "domain", "sub_system_code", "system_code", "tenant_id", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000032', 'svc-domain-test-1.com', 'svc-subsys-domain-test-1', 'svc-system-domain-test-1', '20000000-0000-0000-0000-000000000032', 'from SysDomainServiceTest', true, false);
