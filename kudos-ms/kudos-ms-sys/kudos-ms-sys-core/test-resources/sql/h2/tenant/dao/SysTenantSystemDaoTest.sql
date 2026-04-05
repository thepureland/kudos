-- 测试数据：SysTenantSystemDaoTest（每条 id 唯一；tenant_system 每行 (tenant_id,system_code) 不重复）

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in") values
    ('40000000-0000-0000-0000-000000001699', 'svc-tenant-ts-dao-test-1', null, null, 'from SysTenantSystemDaoTest', true, false),
    ('40000000-0000-0000-0000-000000001700', 'svc-tenant-ts-dao-test-2', null, null, 'from SysTenantSystemDaoTest', true, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in") values
    ('svc-system-ts-dao-test-0_2315', 'svc-system-tenantsubsys-dao-test-1-name', null, false, 'from SysTenantSystemDaoTest', true, false),
    ('svc-subsys-ts-dao-test-1_2315', 'svc-subsys-tenantsubsys-dao-test-1-name', 'svc-system-ts-dao-test-0_2315', true, 'from SysTenantSystemDaoTest', true, false),
    ('svc-subsys-ts-dao-test-2_2315', 'svc-subsys-tenantsubsys-dao-test-2-name', 'svc-system-ts-dao-test-0_2315', true, 'from SysTenantSystemDaoTest', true, false);

merge into "sys_tenant_system" ("id", "tenant_id", "system_code") values
    ('40000000-0000-0000-0000-000000001699', '40000000-0000-0000-0000-000000001699', 'svc-subsys-ts-dao-test-1_2315'),
    ('40000000-0000-0000-0000-000000001700', '40000000-0000-0000-0000-000000001699', 'svc-subsys-ts-dao-test-2_2315'),
    ('40000000-0000-0000-0000-000000001701', '40000000-0000-0000-0000-000000001700', 'svc-subsys-ts-dao-test-1_2315');
