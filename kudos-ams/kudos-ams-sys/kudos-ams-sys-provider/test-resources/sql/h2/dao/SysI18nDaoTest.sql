-- 测试数据：SysI18nDaoTest
-- 使用唯一前缀 svc-i18n-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('svc-as-i18n-dao-test-1', 'svc-as-i18n-dao-test-1-name', 'from SysI18nDaoTest', true, false);


merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "active", "built_in", "create_time")
    values ('40000000-0000-0000-0000-000000000160', 'zh-CN', 'svc-as-i18n-dao-test-1', 'I18N_TYPE_MESSAGE', 'svc-i18n-dao-test-key-1', 'svc-i18n-dao-test-value-1', true, false, '2024-01-01 10:00:00'),
           ('40000000-0000-0000-0000-000000000161', 'en-US', 'svc-as-i18n-dao-test-1', 'I18N_TYPE_MESSAGE', 'svc-i18n-dao-test-key-1', 'svc-i18n-dao-test-value-1-en', true, false, '2024-01-01 10:00:00'),
           ('40000000-0000-0000-0000-000000000162', 'zh-CN', 'svc-as-i18n-dao-test-1', 'I18N_TYPE_LABEL', 'svc-i18n-dao-test-key-2', 'svc-i18n-dao-test-value-2', true, false, '2024-01-01 10:00:00'),
           ('40000000-0000-0000-0000-000000000163', 'zh-CN', 'svc-as-i18n-dao-test-1', 'I18N_TYPE_MESSAGE', 'svc-i18n-dao-test-key-3', 'svc-i18n-dao-test-value-3', false, false, '2024-01-01 10:00:00');
