-- 测试数据：SysDictItemI18nDaoTest
-- 使用唯一前缀 svc-dictitemi18n-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('svc-as-di18n-dao-test-1', 'svc-as-dictitemi18n-dao-test-1-name', 'from SysDictItemI18nDaoTest', true, false);

merge into "sys_module" ("code", "name", "atomic_service_code", "remark", "active", "built_in")
    values ('svc-module-di18n-dao-test-1', 'svc-module-dictitemi18n-dao-test-1-name', 'svc-as-di18n-dao-test-1', 'from SysDictItemI18nDaoTest', true, false);

merge into "sys_dict" ("id", "dict_type", "dict_name", "module_code", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000170', 'svc-dict-di18n-dao-test-1', 'svc-dict-dictitemi18n-dao-test-1-name', 'svc-module-di18n-dao-test-1', 'from SysDictItemI18nDaoTest', true, false);

merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "parent_id", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000171', '40000000-0000-0000-0000-000000000170', 'svc-item-di18n-dao-test-1', 'svc-item-dictitemi18n-dao-test-1-name', 1, null, 'from SysDictItemI18nDaoTest', true, false);

merge into "sys_dict_item_i18n" ("id", "item_id", "locale", "i18n_value", "active", "create_time")
    values ('40000000-0000-0000-0000-000000000172', '40000000-0000-0000-0000-000000000171', 'zh-CN', 'svc-dictitemi18n-dao-test-value-zh', true, '2024-01-01 10:00:00'),
           ('40000000-0000-0000-0000-000000000173', '40000000-0000-0000-0000-000000000171', 'en-US', 'svc-dictitemi18n-dao-test-value-en', true, '2024-01-01 10:00:00'),
           ('40000000-0000-0000-0000-000000000174', '40000000-0000-0000-0000-000000000171', 'ja-JP', 'svc-dictitemi18n-dao-test-value-ja', false, '2024-01-01 10:00:00');
