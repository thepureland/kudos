-- 测试数据：LocaleByCodeCacheTest

merge into "sys_locale" ("id", "code", "display_name", "english_name", "sort_no", "remark", "active", "built_in") values
    ('30000000-0000-0000-0000-000000006001', 'ko_KR', '한국어', 'Korean', 200, 'cache test', true, false),
    ('30000000-0000-0000-0000-000000006002', 'it_IT', 'Italiano', 'Italian', 210, 'cache test', true, false),
    ('30000000-0000-0000-0000-000000006003', 'ru_RU', 'Русский', 'Russian', 220, 'cache test inactive', false, false);
