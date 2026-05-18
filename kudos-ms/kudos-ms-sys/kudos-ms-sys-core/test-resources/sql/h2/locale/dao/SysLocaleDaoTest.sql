-- 测试数据：SysLocaleDaoTest
-- 注意：V1.0.0.23 已内置 zh_CN/zh_TW/en_US 三条 built_in 记录，这里再补两条非内置数据用于测试。

merge into "sys_locale" ("id", "code", "display_name", "english_name", "sort_no", "remark", "active", "built_in") values
    ('30000000-0000-0000-0000-000000004001', 'fr_FR', 'Français', 'French', 40, 'from SysLocaleDaoTest', true, false),
    ('30000000-0000-0000-0000-000000004002', 'de_DE', 'Deutsch', 'German', 50, 'from SysLocaleDaoTest', false, false);
