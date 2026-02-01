-- 测试数据：SysTenantResourceDaoTest
-- 使用唯一前缀 svc-tenantres-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000080', 'svc-tenant-tenantres-dao-test-1', null, null, 'from SysTenantResourceDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000081', 'svc-tenant-tenantres-dao-test-2', null, null, 'from SysTenantResourceDaoTest', true, false);

merge into "sys_portal" ("code", "name", "remark", "active", "built_in")
    values ('svc-portal-tenantres-dao-test-1', 'svc-portal-tenantres-dao-test-1-name', 'from SysTenantResourceDaoTest', true, false);

merge into "sys_sub_system" ("code", "name", "portal_code", "remark", "active", "built_in")
    values ('svc-subsys-tenantres-dao-test-1', 'svc-subsys-tenantres-dao-test-1-name', 'svc-portal-tenantres-dao-test-1', 'from SysTenantResourceDaoTest', true, false);

merge into "sys_resource" ("id", "name", "url", "resource_type_dict_code", "parent_id", "order_num", "icon", "sub_system_code", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000082', 'svc-res-tenantres-dao-test-1', '/svc-res-tenantres-dao-test-1', 'M', null, 1, null, 'svc-subsys-tenantres-dao-test-1', 'from SysTenantResourceDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000083', 'svc-res-tenantres-dao-test-2', '/svc-res-tenantres-dao-test-2', 'B', null, 2, null, 'svc-subsys-tenantres-dao-test-1', 'from SysTenantResourceDaoTest', true, false);

merge into "sys_tenant_resource" ("id", "tenant_id", "resource_id")
    values ('40000000-0000-0000-0000-000000000084', '40000000-0000-0000-0000-000000000080', '40000000-0000-0000-0000-000000000082'),
           ('40000000-0000-0000-0000-000000000085', '40000000-0000-0000-0000-000000000080', '40000000-0000-0000-0000-000000000083'),
           ('40000000-0000-0000-0000-000000000086', '40000000-0000-0000-0000-000000000081', '40000000-0000-0000-0000-000000000082');
