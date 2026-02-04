-- 测试数据：I18NByLocaleAndTypeAndAmsCodeCacheHandler
-- sys_i18n 每条 id 唯一，与用例中的 id 对应

merge into "sys_micro_service" ("code", "name", "remark", "active", "built_in") values
    ('as-i18n-test-1_8910', 'as-i18n-test-1_8910', null, true, false),
    ('as-i18n-test-2_8910', 'as-i18n-test-2_8910', null, true, false);

merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "active", "built_in") values
    ('40000000-0000-0000-0000-000000008901', 'zh_CN', 'as-i18n-test-1_8910', 'label', 'i18n.key.1', 'value-1', true, false),
    ('40000000-0000-0000-0000-000000008902', 'zh_CN', 'as-i18n-test-1_8910', 'label', 'i18n.key.2', 'value-2', true, false),
    ('40000000-0000-0000-0000-000000008903', 'zh_CN', 'as-i18n-test-1_8910', 'label', 'i18n.key.3', 'value-3', false, false),
    ('40000000-0000-0000-0000-000000008910', 'zh_CN', 'as-i18n-test-1_8910', 'label', 'i18n.key.4', 'value-4', true, false),
    ('40000000-0000-0000-0000-000000008911', 'zh_CN', 'as-i18n-test-1_8910', 'label', 'i18n.key.5', 'value-5', true, false),
    ('40000000-0000-0000-0000-000000008912', 'zh_CN', 'as-i18n-test-1_8910', 'label', 'i18n.key.6', 'value-6', true, false),
    ('40000000-0000-0000-0000-000000008913', 'zh_CN', 'as-i18n-test-1_8910', 'label', 'i18n.key.7', 'value-7', false, false),
    ('40000000-0000-0000-0000-000000008914', 'en_US', 'as-i18n-test-1_8910', 'label', 'i18n.key.1', 'value-1-en', true, false),
    ('40000000-0000-0000-0000-000000008915', 'zh_CN', 'as-i18n-test-1_8910', 'message', 'msg.key.1', 'msg-value-1', true, false),
    ('40000000-0000-0000-0000-000000008581', 'en_US', 'as-i18n-test-2_8910', 'label', 'i18n.key.1', 'value-1-as2', true, false),
    ('40000000-0000-0000-0000-000000008582', 'en_US', 'as-i18n-test-2_8910', 'label', 'i18n.key.2', 'value-2-as2', true, false);
