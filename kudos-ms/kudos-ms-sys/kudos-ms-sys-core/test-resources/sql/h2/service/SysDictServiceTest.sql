merge into "sys_micro_service" ("code", "name", "remark", "active", "built_in") values
    ('svc-as-dict-test-1_7514', 'svc-as-dict-test-1_7514-name', 'from SysDictServiceTest', true, false);

merge into "sys_dict" ("id", "atomic_service_code", "dict_type", "dict_name", "remark", "active", "built_in") values
    ('20000000-0000-0000-0000-000000007514', 'svc-module-dict-test-1', 'svc-dict-type-1', 'svc-dict-name-1', 'from SysDictServiceTest', true, false);
