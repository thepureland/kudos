-- 测试数据：I18NByLocaleAndTypeAndAmsCodeCacheHandler

merge into "sys_micro_service" ("code", "name", "remark", "active", "built_in") values
    ('as-i18n-test-1', 'as-i18n-test-1', null, true, false),
    ('as-i18n-test-2', 'as-i18n-test-2', null, true, false);

merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "active", "built_in") values
    ('40000000-0000-0000-0000-000000000201', 'zh_CN', 'as-i18n-test-1', 'label', 'i18n.key.1', 'value-1', true, false),
    ('40000000-0000-0000-0000-000000000202', 'zh_CN', 'as-i18n-test-1', 'label', 'i18n.key.2', 'value-2', true, false),
    ('40000000-0000-0000-0000-000000000203', 'zh_CN', 'as-i18n-test-1', 'label', 'i18n.key.3', 'value-3', false, false),
    ('40000000-0000-0000-0000-000000000204', 'en_US', 'as-i18n-test-1', 'label', 'i18n.key.1', 'value-1-en', true, false),
    ('40000000-0000-0000-0000-000000000205', 'zh_CN', 'as-i18n-test-1', 'message', 'msg.key.1', 'msg-value-1', true, false),
    ('40000000-0000-0000-0000-000000000206', 'zh_CN', 'as-i18n-test-2', 'label', 'i18n.key.1', 'value-1-as2', true, false),
    ('40000000-0000-0000-0000-000000000207', 'en_US', 'as-i18n-test-2', 'label', 'i18n.key.2', 'value-2-as2', true, false),
    ('40000000-0000-0000-0000-000000000208', 'zh_CN', 'as-i18n-test-1', 'label', 'i18n.key.4', 'value-4', true, false),
    ('40000000-0000-0000-0000-000000000209', 'zh_CN', 'as-i18n-test-1', 'label', 'i18n.key.5', 'value-5', true, false),
    ('40000000-0000-0000-0000-000000000210', 'zh_CN', 'as-i18n-test-1', 'label', 'i18n.key.6', 'value-6', true, false),
    ('40000000-0000-0000-0000-000000000211', 'zh_CN', 'as-i18n-test-1', 'label', 'i18n.key.7', 'value-7', false, false);
