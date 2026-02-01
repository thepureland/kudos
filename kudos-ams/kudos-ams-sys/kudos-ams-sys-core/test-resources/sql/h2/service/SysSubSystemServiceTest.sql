merge into "sys_system" ("code", "name", "remark", "active", "built_in")
    values ('svc-system-subsystem-test-1', 'svc-system-subsystem-test-1-name', 'from SysSubSystemServiceTest', true, false);

merge into "sys_sub_system" ("code", "name", "system_code", "remark", "active", "built_in")
    values ('svc-subsystem-test-1', 'svc-subsystem-test-1-name', 'svc-system-subsystem-test-1', 'from SysSubSystemServiceTest', true, false);

merge into "sys_micro_service" ("code", "name", "context", "remark", "active", "built_in")
    values ('svc-ms-subsys-test-1', 'svc-microservice-subsystem-test-1-name', '/svc-ms-subsys-test-1', 'from SysSubSystemServiceTest', true, false);

merge into "sys_sub_system_micro_service" ("id", "sub_system_code", "micro_service_code")
    values ('10000000-0000-0000-0000-000000000020', 'svc-subsystem-test-1', 'svc-ms-subsys-test-1');

