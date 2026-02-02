-- 测试数据：SysMicroServiceAtomicServiceDaoTest
-- 使用唯一前缀 svc-msas-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_system" ("code", "name", "remark", "active", "built_in")
    values ('svc-system-msas-dao-test-1', 'svc-system-msas-dao-test-1-name', 'from SysMicroServiceAtomicServiceDaoTest', true, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in")
    values ('svc-subsys-msas-dao-test-1', 'svc-subsys-msas-dao-test-1-name', 'svc-system-msas-dao-test-1', true, 'from SysMicroServiceAtomicServiceDaoTest', true, false);

merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('svc-as-msas-dao-test-1', 'svc-as-msas-dao-test-1-name', 'from SysMicroServiceAtomicServiceDaoTest', true, false),
           ('svc-as-msas-dao-test-2', 'svc-as-msas-dao-test-2-name', 'from SysMicroServiceAtomicServiceDaoTest', true, false);

merge into "sys_micro_service" ("code", "name", "context", "remark", "active", "built_in")
    values ('svc-ms-msas-dao-test-1', 'svc-ms-msas-dao-test-1-name', '/svc-ms-msas-dao-1', 'from SysMicroServiceAtomicServiceDaoTest', true, false),
           ('svc-ms-msas-dao-test-2', 'svc-ms-msas-dao-test-2-name', '/svc-ms-msas-dao-2', 'from SysMicroServiceAtomicServiceDaoTest', true, false);

merge into "sys_micro_service_atomic_service" ("id", "micro_service_code", "atomic_service_code")
    values ('40000000-0000-0000-0000-000000000060', 'svc-ms-msas-dao-test-1', 'svc-as-msas-dao-test-1'),
           ('40000000-0000-0000-0000-000000000061', 'svc-ms-msas-dao-test-1', 'svc-as-msas-dao-test-2'),
           ('40000000-0000-0000-0000-000000000062', 'svc-ms-msas-dao-test-2', 'svc-as-msas-dao-test-1');
