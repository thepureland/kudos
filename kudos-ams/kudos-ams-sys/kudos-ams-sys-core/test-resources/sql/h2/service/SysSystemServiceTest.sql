merge into "sys_system" ("code", "name", "remark", "active", "built_in")
    values ('svc-system-test-1', 'svc-system-test-1-name', 'from SysSystemServiceTest', true, false);

merge into "sys_sub_system" ("code", "name", "system_code", "remark", "active", "built_in")
    values ('svc-subsystem-test-system-1', 'svc-subsystem-test-system-1-name', 'svc-system-test-1', 'from SysSystemServiceTest', true, false);

