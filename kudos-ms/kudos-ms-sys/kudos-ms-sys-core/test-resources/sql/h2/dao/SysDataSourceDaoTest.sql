-- 测试数据：SysDataSourceDaoTest
-- 使用唯一前缀 svc-datasource-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in") values
    ('40000000-0000-0000-0000-000000000200', 'svc-tenant-ds-dao-test-1', null, null, 'from SysDataSourceDaoTest', true, false);

merge into "sys_system" ("code", "name", "sub_system", "parent_code", "remark", "active", "built_in") values
    ('svc-system-ds-dao-test-0', 'svc-system-datasource-dao-test-1-name', false, null, 'from SysDataSourceDaoTest', true, false),
    ('svc-subsys-ds-dao-test-1', 'svc-subsys-datasource-dao-test-1-name', true, 'svc-system-ds-dao-test-0', 'from SysDataSourceDaoTest', true, false);

merge into "sys_micro_service" ("code", "name", "context", "atomic_service", "parent_code", "remark", "active", "built_in") values
    ('svc-ms-ds-dao-test-0', 'svc-ms-datasource-dao-test-1-name', '/svc-ms-ds-dao-test-1', false, null, 'from SysDataSourceDaoTest', true, false),
    ('svc-as-ds-dao-test-1', 'svc-as-datasource-dao-test-1-name', '/', true, 'svc-ms-ds-dao-test-0', 'from SysDataSourceDaoTest', true, false);

merge into "sys_data_source" ("id", "name", "sub_system_code", "micro_service_code", "tenant_id", "url", "username", "password", "active", "built_in") values
    ('40000000-0000-0000-0000-000000000201', 'svc-datasource-dao-test-1', 'svc-subsys-ds-dao-test-1', 'svc-ms-ds-dao-test-1', '40000000-0000-0000-0000-000000000200', 'jdbc:h2:mem:test1', 'sa', 'sa', true, false),
    ('40000000-0000-0000-0000-000000000202', 'svc-datasource-dao-test-2', 'svc-subsys-ds-dao-test-1', 'svc-ms-ds-dao-test-1', null, 'jdbc:h2:mem:test2', 'sa', 'sa', true, false);
