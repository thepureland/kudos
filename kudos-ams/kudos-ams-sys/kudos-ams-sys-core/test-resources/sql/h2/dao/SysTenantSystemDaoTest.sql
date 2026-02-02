-- 测试数据：SysTenantSystemDaoTest
-- 使用唯一前缀 svc-tenantsubsys-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000070', 'svc-tenant-ts-dao-test-1', null, null, 'from SysTenantSystemDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000071', 'svc-tenant-ts-dao-test-2', null, null, 'from SysTenantSystemDaoTest', true, false);

merge into "sys_system" ("code", "name", "remark", "active", "built_in")
    values ('svc-system-ts-dao-test-1', 'svc-system-tenantsubsys-dao-test-1-name', 'from SysTenantSystemDaoTest', true, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in")
    values ('svc-subsys-ts-dao-test-1', 'svc-subsys-tenantsubsys-dao-test-1-name', 'svc-system-ts-dao-test-1', true, 'from SysTenantSystemDaoTest', true, false),
           ('svc-subsys-ts-dao-test-2', 'svc-subsys-tenantsubsys-dao-test-2-name', 'svc-system-ts-dao-test-1', true, 'from SysTenantSystemDaoTest', true, false);

merge into "sys_tenant_system" ("id", "tenant_id", "system_code")
    values ('40000000-0000-0000-0000-000000000072', '40000000-0000-0000-0000-000000000070', 'svc-subsys-ts-dao-test-1'),
           ('40000000-0000-0000-0000-000000000073', '40000000-0000-0000-0000-000000000070', 'svc-subsys-ts-dao-test-2'),
           ('40000000-0000-0000-0000-000000000074', '40000000-0000-0000-0000-000000000071', 'svc-subsys-ts-dao-test-1');
