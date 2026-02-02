-- 测试数据：SysTenantResourceDaoTest（每条 id 唯一；tenant_resource 每行 (tenant_id,resource_id) 不重复）

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in") values
    ('40000000-0000-0000-0000-000000004229', 'svc-tenant-tenantres-dao-test-1', null, null, 'from SysTenantResourceDaoTest', true, false),
    ('40000000-0000-0000-0000-000000004230', 'svc-tenant-tenantres-dao-test-2', null, null, 'from SysTenantResourceDaoTest', true, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in") values
    ('svc-system-tenantres-dao-te_9870', 'svc-system-tenantres-dao-test-1-name', null, false, 'from SysTenantResourceDaoTest', true, false),
    ('svc-subsys-tenantres-dao-te_9870', 'svc-subsys-tenantres-dao-te_9870-name', 'svc-system-tenantres-dao-te_9870', true, 'from SysTenantResourceDaoTest', true, false);

merge into "sys_resource" ("id", "name", "url", "resource_type_dict_code", "parent_id", "order_num", "icon", "sub_system_code", "remark", "active", "built_in") values
    ('40000000-0000-0000-0000-000000004229', 'svc-res-tenantres-dao-test-1', '/svc-res-tenantres-dao-test-1', 'M', null, 1, null, 'svc-subsys-tenantres-dao-te_9870', 'from SysTenantResourceDaoTest', true, false),
    ('40000000-0000-0000-0000-000000004230', 'svc-res-tenantres-dao-test-2', '/svc-res-tenantres-dao-test-2', 'B', null, 2, null, 'svc-subsys-tenantres-dao-te_9870', 'from SysTenantResourceDaoTest', true, false);

merge into "sys_tenant_resource" ("id", "tenant_id", "resource_id") values
    ('40000000-0000-0000-0000-000000004229', '40000000-0000-0000-0000-000000004229', '40000000-0000-0000-0000-000000004229'),
    ('40000000-0000-0000-0000-000000004230', '40000000-0000-0000-0000-000000004229', '40000000-0000-0000-0000-000000004230'),
    ('40000000-0000-0000-0000-000000004231', '40000000-0000-0000-0000-000000004230', '40000000-0000-0000-0000-000000004229');
