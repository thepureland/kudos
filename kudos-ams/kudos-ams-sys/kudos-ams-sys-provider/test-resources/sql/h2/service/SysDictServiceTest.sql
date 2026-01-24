merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('svc-as-dict-test-1', 'svc-as-dict-test-1-name', 'from SysDictServiceTest', true, false);

merge into "sys_module" ("code", "name", "atomic_service_code", "remark", "active", "built_in")
    values ('svc-module-dict-test-1', 'svc-module-dict-test-1-name', 'svc-as-dict-test-1', 'from SysDictServiceTest', true, false);

merge into "sys_dict" ("id", "module_code", "dict_type", "dict_name", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000028', 'svc-module-dict-test-1', 'svc-dict-type-1', 'svc-dict-name-1', 'from SysDictServiceTest', true, false);
