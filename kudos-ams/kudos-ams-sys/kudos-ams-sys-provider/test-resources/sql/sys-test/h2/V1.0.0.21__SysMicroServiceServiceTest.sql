merge into "sys_micro_service" ("code", "name", "context", "remark", "active", "built_in")
    values ('svc-microservice-test-1', 'svc-microservice-test-1-name', '/svc-microservice-test-1', 'from SysMicroServiceServiceTest', true, false);

merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('svc-as-ms-test-1', 'svc-atomicservice-microservice-test-1-name', 'from SysMicroServiceServiceTest', true, false);

merge into "sys_micro_service_atomic_service" ("id", "micro_service_code", "atomic_service_code")
    values ('10000000-0000-0000-0000-000000000021', 'svc-microservice-test-1', 'svc-as-ms-test-1');

