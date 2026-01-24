merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('svc-as-dicti18n-test-1', 'svc-as-dicti18n-test-1-name', 'from SysDictItemI18NServiceTest', true, false);

merge into "sys_module" ("code", "name", "atomic_service_code", "remark", "active", "built_in")
    values ('svc-module-dicti18n-test-1', 'svc-module-dicti18n-test-1-name', 'svc-as-dicti18n-test-1', 'from SysDictItemI18NServiceTest', true, false);

merge into "sys_dict" ("id", "module_code", "dict_type", "dict_name", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000030', 'svc-module-dicti18n-test-1', 'svc-dict-type-i18n-1', 'svc-dict-name-i18n-1', 'from SysDictItemI18NServiceTest', true, false);

merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "parent_id", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000031', '20000000-0000-0000-0000-000000000030', 'svc-item-i18n-1', 'svc-item-i18n-1-name', 1, null, 'from SysDictItemI18NServiceTest', true, false);

merge into "sys_dict_item_i18n" ("id", "item_id", "locale", "i18n_value", "active")
    values ('20000000-0000-0000-0000-000000000031', '20000000-0000-0000-0000-000000000031', 'zh_CN', '中文值', true),
           ('20000000-0000-0000-0000-000000000032', '20000000-0000-0000-0000-000000000031', 'en_US', 'English Value', true);
