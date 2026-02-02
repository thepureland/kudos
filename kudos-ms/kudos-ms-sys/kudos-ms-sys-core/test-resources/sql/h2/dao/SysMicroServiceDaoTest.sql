-- 测试数据：SysMicroServiceDaoTest
-- 使用唯一前缀 svc-microservice-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_micro_service" ("code", "name", "context", "remark", "active", "built_in") values
    ('svc-microservice-dao-test-1_2459', 'svc-microservice-dao-test-1_2459-name', '/svc-ms-dao-test-1', 'from SysMicroServiceDaoTest', true, false),
    ('svc-microservice-dao-test-2_2459', 'svc-microservice-dao-test-2_2459-name', '/svc-ms-dao-test-2', 'from SysMicroServiceDaoTest', true, false);
