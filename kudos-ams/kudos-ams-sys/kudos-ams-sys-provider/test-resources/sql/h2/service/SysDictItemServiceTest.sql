merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('svc-as-dictitem-test-1', 'svc-as-dictitem-test-1-name', 'from SysDictItemServiceTest', true, false);

merge into "sys_module" ("code", "name", "atomic_service_code", "remark", "active", "built_in")
    values ('svc-module-dictitem-test-1', 'svc-module-dictitem-test-1-name', 'svc-as-dictitem-test-1', 'from SysDictItemServiceTest', true, false);

merge into "sys_dict" ("id", "module_code", "dict_type", "dict_name", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000029', 'svc-module-dictitem-test-1', 'svc-dict-type-item-1', 'svc-dict-name-item-1', 'from SysDictItemServiceTest', true, false);

merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "parent_id", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000029', '20000000-0000-0000-0000-000000000029', 'svc-item-code-1', 'svc-item-name-1', 1, null, 'from SysDictItemServiceTest', true, false),
           ('20000000-0000-0000-0000-000000000030', '20000000-0000-0000-0000-000000000029', 'svc-item-code-2', 'svc-item-name-2', 2, '20000000-0000-0000-0000-000000000029', 'from SysDictItemServiceTest', true, false);
