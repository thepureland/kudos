merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000026', 'svc-tenant-ds-test-1', null, null, 'from SysDataSourceServiceTest', true, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in")
    values ('svc-subsys-ds-test-1', 'svc-subsys-ds-test-1-name', 'default', true, 'from SysDataSourceServiceTest', true, false);

merge into "sys_micro_service" ("code", "name", "context", "remark", "active", "built_in")
    values ('svc-ms-ds-test-1', 'svc-ms-ds-test-1-name', '/svc-ms-ds-test-1', 'from SysDataSourceServiceTest', true, false);

merge into "sys_data_source" ("id", "name", "sub_system_code", "micro_service_code", "tenant_id", "url", "username", "password", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000026', 'svc-ds-test-1', 'svc-subsys-ds-test-1', 'svc-ms-ds-test-1', '20000000-0000-0000-0000-000000000026', 'jdbc:h2:mem:test', 'sa', 'sa', 'from SysDataSourceServiceTest', true, false);
