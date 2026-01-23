merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('svc-as-module-test-1', 'svc-as-module-test-1-name', 'from SysModuleServiceTest', true, false);

merge into "sys_module" ("code", "name", "atomic_service_code", "remark", "active", "built_in")
    values ('svc-module-test-1', 'svc-module-test-1-name', 'svc-as-module-test-1', 'from SysModuleServiceTest', true, false);
