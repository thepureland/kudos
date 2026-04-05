merge into "sys_system" ("code", "name", "remark", "active", "built_in") values
    ('svc-system-ssms-test-1_5473', 'svc-system-ssms-test-1_5473-name', 'from SysSubSystemMicroServiceServiceTest', true, false);

merge into "sys_micro_service" ("code", "name", "context", "remark", "active", "built_in") values
    ('svc-ms-ssms-test-1_5473', 'svc-ms-ssms-test-1_5473-name', '/svc-ms-ssms-test-1_5473', 'from SysSubSystemMicroServiceServiceTest', true, false);

merge into "sys_sub_system_micro_service" ("id", "sub_system_code", "micro_service_code") values
    ('20000000-0000-0000-0000-000000005473', 'svc-subsys-ssms-test-1', 'svc-ms-ssms-test-1_5473');
