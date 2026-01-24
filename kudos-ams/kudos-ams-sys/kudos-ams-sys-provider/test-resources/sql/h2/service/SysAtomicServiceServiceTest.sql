merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('svc-atomicservice-test-1', 'svc-atomicservice-test-1-name', 'from SysAtomicServiceServiceTest', true, false);

merge into "sys_module" ("code", "name", "atomic_service_code", "remark", "active", "built_in")
    values ('svc-module-as-test-1', 'svc-module-atomicservice-test-1-name', 'svc-atomicservice-test-1', 'from SysAtomicServiceServiceTest', true, false);

