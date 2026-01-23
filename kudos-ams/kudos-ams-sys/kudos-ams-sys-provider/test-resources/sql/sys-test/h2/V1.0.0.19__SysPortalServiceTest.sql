merge into "sys_portal" ("code", "name", "remark", "active", "built_in")
    values ('svc-portal-test-1', 'svc-portal-test-1-name', 'from SysPortalServiceTest', true, false);

merge into "sys_sub_system" ("code", "name", "portal_code", "remark", "active", "built_in")
    values ('svc-subsystem-test-portal-1', 'svc-subsystem-test-portal-1-name', 'svc-portal-test-1', 'from SysPortalServiceTest', true, false);

