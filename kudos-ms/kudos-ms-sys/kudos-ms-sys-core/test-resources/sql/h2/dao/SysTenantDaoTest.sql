-- 测试数据：SysTenantDaoTest
-- 使用唯一前缀 svc-tenant-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in") values
    ('40000000-0000-0000-0000-000000007186', 'svc-tenant-dao-test-1', 'Asia/Shanghai', 'zh-CN', 'from SysTenantDaoTest', true, false),
    ('40000000-0000-0000-0000-000000007186', 'svc-tenant-dao-test-2', 'America/New_York', 'en-US', 'from SysTenantDaoTest', true, false);
