-- 测试数据：SysTenantSubSystemDaoTest
-- 使用唯一前缀 svc-tenantsubsys-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000070', 'svc-tenant-ts-dao-test-1', null, null, 'from SysTenantSubSystemDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000071', 'svc-tenant-ts-dao-test-2', null, null, 'from SysTenantSubSystemDaoTest', true, false);

merge into "sys_system" ("code", "name", "remark", "active", "built_in")
    values ('svc-system-ts-dao-test-1', 'svc-system-tenantsubsys-dao-test-1-name', 'from SysTenantSubSystemDaoTest', true, false);

merge into "sys_sub_system" ("code", "name", "system_code", "remark", "active", "built_in")
    values ('svc-subsys-ts-dao-test-1', 'svc-subsys-tenantsubsys-dao-test-1-name', 'svc-system-ts-dao-test-1', 'from SysTenantSubSystemDaoTest', true, false),
           ('svc-subsys-ts-dao-test-2', 'svc-subsys-tenantsubsys-dao-test-2-name', 'svc-system-ts-dao-test-1', 'from SysTenantSubSystemDaoTest', true, false);

merge into "sys_tenant_sub_system" ("id", "tenant_id", "sub_system_code", "system_code")
    values ('40000000-0000-0000-0000-000000000072', '40000000-0000-0000-0000-000000000070', 'svc-subsys-ts-dao-test-1', 'svc-system-ts-dao-test-1'),
           ('40000000-0000-0000-0000-000000000073', '40000000-0000-0000-0000-000000000070', 'svc-subsys-ts-dao-test-2', 'svc-system-ts-dao-test-1'),
           ('40000000-0000-0000-0000-000000000074', '40000000-0000-0000-0000-000000000071', 'svc-subsys-ts-dao-test-1', 'svc-system-ts-dao-test-1');
