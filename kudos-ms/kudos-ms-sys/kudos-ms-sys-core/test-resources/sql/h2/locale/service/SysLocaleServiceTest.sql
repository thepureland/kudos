-- 测试数据：SysLocaleServiceTest

merge into "sys_locale" ("id", "code", "display_name", "english_name", "sort_no", "remark", "active", "built_in") values
    ('30000000-0000-0000-0000-000000005001', 'ja_JP', '日本語', 'Japanese', 100, 'active from SysLocaleServiceTest', true, false),
    ('30000000-0000-0000-0000-000000005002', 'es_ES', 'Español', 'Spanish', 110, 'inactive from SysLocaleServiceTest', false, false);
