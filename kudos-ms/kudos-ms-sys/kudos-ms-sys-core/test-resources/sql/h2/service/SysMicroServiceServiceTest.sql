-- 微服务 svc-microservice-test-1_2407 下挂原子服务 svc-as-ms-test-1_2407，供 getAtomicServicesByMicroServiceCode 查询
merge into "sys_micro_service" ("code", "name", "context", "atomic_service", "parent_code", "remark", "active", "built_in") values
('svc-microservice-test-1_2407', 'svc-microservice-test-1_2407-name', '/svc-microservice-test-1_2407', false, null, 'from SysMicroServiceServiceTest', true, false),
('svc-as-ms-test-1_2407', 'svc-atomicservice-microservice-test-1-name', '/', true, 'svc-microservice-test-1_2407', 'from SysMicroServiceServiceTest', true, false);
