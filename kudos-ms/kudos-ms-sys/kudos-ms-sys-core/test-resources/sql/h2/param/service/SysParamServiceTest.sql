merge into "sys_micro_service" ("code", "name", "remark", "active", "built_in") values
    ('svc-as-param-test-1_8393', 'svc-as-param-test-1_8393-name', 'from SysParamServiceTest', true, false);

merge into "sys_param" ("id", "param_name", "param_value", "default_value", "atomic_service_code", "order_num", "remark", "active", "built_in") values
    ('20000000-0000-0000-0000-000000008393', 'svc-param-name-1', 'svc-param-value-1', 'svc-param-default-1', 'svc-module-param-test-1', 1, 'from SysParamServiceTest', true, false);
