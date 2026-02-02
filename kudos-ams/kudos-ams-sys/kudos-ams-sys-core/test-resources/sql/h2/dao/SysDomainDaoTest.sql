-- 测试数据：SysDomainDaoTest
-- 使用唯一前缀 svc-domain-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000190', 'svc-tenant-domain-dao-test-1', null, null, 'from SysDomainDaoTest', true, false);

merge into "sys_system" ("code", "name", "remark", "active", "built_in")
    values ('svc-system-domain-dao-test-1', 'svc-system-domain-dao-test-1-name', 'from SysDomainDaoTest', true, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in")
    values ('svc-subsys-domain-dao-test-1', 'svc-subsys-domain-dao-test-1-name', 'svc-system-domain-dao-test-1', true, 'from SysDomainDaoTest', true, false);

merge into "sys_domain" ("id", "domain", "system_code", "tenant_id", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000191', 'svc-domain-dao-test-1.com', 'svc-system-domain-dao-test-1', '40000000-0000-0000-0000-000000000190', 'from SysDomainDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000192', 'svc-domain-dao-test-2.com', 'svc-system-domain-dao-test-1', '40000000-0000-0000-0000-000000000190', 'from SysDomainDaoTest', true, false);
