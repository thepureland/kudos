merge into "sys_micro_service" ("code", "name", "context", "atomic_service", "parent_code", "remark", "active", "built_in") values
    ('svc-microservice-test-1', 'svc-microservice-test-1-name', '/svc-microservice-test-1', false, null, 'from SysMicroServiceServiceTest', true, false),
    ('svc-as-ms-test-1', 'svc-atomicservice-microservice-test-1-name', '/', false, null, 'from SysMicroServiceServiceTest', true, false),
    ('10000000-0000-0000-0000-00000021', 'svc-microservice-test-1', '/', true, 'svc-as-ms-test-1', 'svc-as-ms-test-1', true, false);
