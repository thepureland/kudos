merge into "sys_system" ("code", "name", "remark", "active", "built_in", "sub_system") values
    ('svc-system-test-1_6368', 'svc-system-test-1_6368-name', 'from SysSystemServiceTest', true, false, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in") values
    ('svc-subsystem-test-system-1_6368', 'svc-subsystem-test-system-1_6368-name', 'svc-system-test-1_6368', true, 'from SysSystemServiceTest', true, false);
