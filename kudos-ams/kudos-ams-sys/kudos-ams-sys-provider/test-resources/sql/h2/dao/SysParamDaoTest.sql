-- 测试数据：SysParamDaoTest
-- 使用唯一前缀 svc-param-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('svc-as-param-dao-test-1', 'svc-as-param-dao-test-1-name', 'from SysParamDaoTest', true, false);


merge into "sys_param" ("id", "param_name", "param_value", "default_value", "atomic_service_code", "order_num", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000041', 'svc-param-dao-test-1', 'svc-param-value-1', 'svc-param-default-1', 'svc-module-param-dao-test-1', 1, 'from SysParamDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000042', 'svc-param-dao-test-2', 'svc-param-value-2', 'svc-param-default-2', 'svc-module-param-dao-test-1', 2, 'from SysParamDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000043', 'svc-param-dao-test-3', 'svc-param-value-3', 'svc-param-default-3', '', 3, 'from SysParamDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000044', 'svc-param-dao-test-4', 'svc-param-value-4', 'svc-param-default-4', 'svc-module-param-dao-test-1', 4, 'from SysParamDaoTest', false, false);
