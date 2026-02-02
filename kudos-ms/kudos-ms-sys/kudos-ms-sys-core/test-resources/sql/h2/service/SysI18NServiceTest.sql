-- 测试数据：SysI18NServiceTest（每条 id 唯一）

merge into "sys_micro_service" ("code", "name", "remark", "active", "built_in") values
('svc-as-i18n-test-1_8449', 'svc-as-i18n-test-1_8449-name', 'from SysI18NServiceTest', true, false);

merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "active", "built_in") values
('20000000-0000-0000-0000-000000008449', 'zh_CN', 'svc-as-i18n-test-1_8449', 'label', 'svc-i18n-key-1', 'svc-i18n-value-1', true, false),
('20000000-0000-0000-0000-000000008450', 'en_US', 'svc-as-i18n-test-1_8449', 'label', 'svc-i18n-key-1', 'svc-i18n-value-en', true, false);
