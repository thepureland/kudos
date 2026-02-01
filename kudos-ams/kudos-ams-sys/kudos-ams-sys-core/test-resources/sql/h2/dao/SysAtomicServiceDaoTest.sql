-- 测试数据：SysAtomicServiceDaoTest
-- 使用唯一前缀 svc-atomicservice-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('svc-atomicsvc-dao-test-1', 'svc-atomicservice-dao-test-1-name', 'from SysAtomicServiceDaoTest', true, false),
           ('svc-atomicsvc-dao-test-2', 'svc-atomicservice-dao-test-2-name', 'from SysAtomicServiceDaoTest', true, false);
