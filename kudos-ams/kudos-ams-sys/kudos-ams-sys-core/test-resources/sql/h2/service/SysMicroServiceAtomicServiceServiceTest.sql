merge into "sys_micro_service" ("code", "name", "context", "remark", "active", "built_in")
    values ('svc-ms-msas-test-1', 'svc-ms-msas-test-1-name', '/svc-ms-msas-test-1', 'from SysMicroServiceAtomicServiceServiceTest', true, false);

merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('svc-as-msas-test-1', 'svc-as-msas-test-1-name', 'from SysMicroServiceAtomicServiceServiceTest', true, false);

merge into "sys_micro_service_atomic_service" ("id", "micro_service_code", "atomic_service_code")
    values ('20000000-0000-0000-0000-000000000037', 'svc-ms-msas-test-1', 'svc-as-msas-test-1');
