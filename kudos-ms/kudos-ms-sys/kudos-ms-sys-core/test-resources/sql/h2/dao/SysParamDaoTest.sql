-- 测试数据：SysParamDaoTest（每条 id 唯一）

merge into "sys_micro_service" ("code", "name", "remark", "active", "built_in") values
    ('svc-as-param-dao-test-1_9823', 'svc-as-param-dao-test-1_9823-name', 'from SysParamDaoTest', true, false);

merge into "sys_param" ("id", "param_name", "param_value", "default_value", "atomic_service_code", "order_num", "remark", "active", "built_in") values
    ('40000000-0000-0000-0000-000000009823', 'svc-param-dao-test-1', 'svc-param-value-1', 'svc-param-default-1', 'svc-module-param-dao-test-1', 1, 'from SysParamDaoTest', true, false),
    ('40000000-0000-0000-0000-000000009824', 'svc-param-dao-test-2', 'svc-param-value-2', 'svc-param-default-2', 'svc-module-param-dao-test-1', 2, 'from SysParamDaoTest', true, false),
    ('40000000-0000-0000-0000-000000009825', 'svc-param-dao-test-3', 'svc-param-value-3', 'svc-param-default-3', '', 3, 'from SysParamDaoTest', true, false),
    ('40000000-0000-0000-0000-000000009826', 'svc-param-dao-test-4', 'svc-param-value-4', 'svc-param-default-4', 'svc-module-param-dao-test-1', 4, 'from SysParamDaoTest', false, false);
