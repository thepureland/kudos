merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('svc-as-param-test-1', 'svc-as-param-test-1-name', 'from SysParamServiceTest', true, false);

merge into "sys_module" ("code", "name", "atomic_service_code", "remark", "active", "built_in")
    values ('svc-module-param-test-1', 'svc-module-param-test-1-name', 'svc-as-param-test-1', 'from SysParamServiceTest', true, false);

merge into "sys_param" ("id", "param_name", "param_value", "default_value", "module_code", "order_num", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000038', 'svc-param-name-1', 'svc-param-value-1', 'svc-param-default-1', 'svc-module-param-test-1', 1, 'from SysParamServiceTest', true, false);
