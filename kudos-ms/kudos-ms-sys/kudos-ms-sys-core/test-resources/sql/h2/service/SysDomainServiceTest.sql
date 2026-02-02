-- 测试数据：SysDomainServiceTest（id/tenant_id 与用例一致：domain.id=1461, tenant_id=1461）

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in") values
('20000000-0000-0000-0000-000000001461', 'svc-tenant-domain-test-1', null, null, 'from SysDomainServiceTest', true, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in") values
('svc-system-domain-test-0_1556', 'svc-system-domain-test-0-name', null, false, 'from SysDomainServiceTest', true, false),
('svc-system-domain-test-1', 'svc-system-domain-test-1-name', 'svc-system-domain-test-0_1556', true, 'from SysDomainServiceTest', true, false),
('svc-subsys-domain-test-1_1556', 'svc-subsys-domain-test-1_1556-name', 'svc-system-domain-test-0_1556', true, 'from SysDomainServiceTest', true, false);

merge into "sys_domain" ("id", "domain", "system_code", "tenant_id", "remark", "active", "built_in") values
('20000000-0000-0000-0000-000000001461', 'svc-domain-test-1.com', 'svc-system-domain-test-1', '20000000-0000-0000-0000-000000001461', 'from SysDomainServiceTest', true, false);
