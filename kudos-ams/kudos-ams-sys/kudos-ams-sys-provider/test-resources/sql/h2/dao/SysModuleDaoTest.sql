-- 测试数据：SysModuleDaoTest
-- 使用唯一前缀 svc-module-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('svc-as-module-dao-test-1', 'svc-as-module-dao-test-1-name', 'from SysModuleDaoTest', true, false);

merge into "sys_module" ("code", "name", "atomic_service_code", "remark", "active", "built_in")
    values ('svc-module-dao-test-1', 'svc-module-dao-test-1-name', 'svc-as-module-dao-test-1', 'from SysModuleDaoTest', true, false),
           ('svc-module-dao-test-2', 'svc-module-dao-test-2-name', 'svc-as-module-dao-test-1', 'from SysModuleDaoTest', true, false);
