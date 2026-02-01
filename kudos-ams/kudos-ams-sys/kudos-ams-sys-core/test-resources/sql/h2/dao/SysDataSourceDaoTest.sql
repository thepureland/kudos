-- 测试数据：SysDataSourceDaoTest
-- 使用唯一前缀 svc-datasource-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000200', 'svc-tenant-ds-dao-test-1', null, null, 'from SysDataSourceDaoTest', true, false);

merge into "sys_portal" ("code", "name", "remark", "active", "built_in")
    values ('svc-portal-ds-dao-test-1', 'svc-portal-datasource-dao-test-1-name', 'from SysDataSourceDaoTest', true, false);

merge into "sys_sub_system" ("code", "name", "portal_code", "remark", "active", "built_in")
    values ('svc-subsys-ds-dao-test-1', 'svc-subsys-datasource-dao-test-1-name', 'svc-portal-ds-dao-test-1', 'from SysDataSourceDaoTest', true, false);

merge into "sys_micro_service" ("code", "name", "context", "remark", "active", "built_in")
    values ('svc-ms-ds-dao-test-1', 'svc-ms-datasource-dao-test-1-name', '/svc-ms-ds-dao-test-1', 'from SysDataSourceDaoTest', true, false);

merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('svc-as-ds-dao-test-1', 'svc-as-datasource-dao-test-1-name', 'from SysDataSourceDaoTest', true, false);

merge into "sys_data_source" ("id", "name", "sub_system_code", "micro_service_code", "atomic_service_code", "tenant_id", "url", "username", "password", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000201', 'svc-datasource-dao-test-1', 'svc-subsys-ds-dao-test-1', 'svc-ms-ds-dao-test-1', 'svc-as-ds-dao-test-1', '40000000-0000-0000-0000-000000000200', 'jdbc:h2:mem:test1', 'sa', 'sa', true, false),
           ('40000000-0000-0000-0000-000000000202', 'svc-datasource-dao-test-2', 'svc-subsys-ds-dao-test-1', 'svc-ms-ds-dao-test-1', 'svc-as-ds-dao-test-1', null, 'jdbc:h2:mem:test2', 'sa', 'sa', true, false);
