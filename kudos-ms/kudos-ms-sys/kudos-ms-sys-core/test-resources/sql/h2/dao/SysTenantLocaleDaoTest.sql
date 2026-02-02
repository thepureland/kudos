-- 测试数据：SysTenantLocaleDaoTest
-- 使用唯一前缀 svc-tenantlang-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000090', 'svc-tenant-tlang-dao-test-1', null, null, 'from SysTenantLocaleDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000091', 'svc-tenant-tlang-dao-test-2', null, null, 'from SysTenantLocaleDaoTest', true, false);

merge into "sys_tenant_locale" ("id", "tenant_id", "locale_code")
    values ('40000000-0000-0000-0000-000000000092', '40000000-0000-0000-0000-000000000090', 'zh-CN'),
           ('40000000-0000-0000-0000-000000000093', '40000000-0000-0000-0000-000000000090', 'en-US'),
           ('40000000-0000-0000-0000-000000000094', '40000000-0000-0000-0000-000000000091', 'zh-CN');
