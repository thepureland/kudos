merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in") values
    ('218772a0-c053-4634-a5e5-111111118781', 'tenant-11', null, null, null, true, true),
    ('218772a0-c053-4634-a5e5-222222228781', 'tenant-22', null, null, null, true, true),
    ('218772a0-c053-4634-a5e5-333333338781', 'tenant-33', null, null, null, true, true),
    ('218772a0-c053-4634-a5e5-444444448781', 'tenant-44', null, null, null, true, true),
    ('218772a0-c053-4634-a5e5-555555558781', 'tenant-55', null, null, null, true, true),
    ('218772a0-c053-4634-a5e5-666666668781', 'tenant-66', null, null, null, true, true),
    ('218772a0-c053-4634-a5e5-777777778781', 'tenant-77', null, null, null, true, true),
    ('218772a0-c053-4634-a5e5-888888888781', 'tenant-88', null, null, null, true, true),
    ('218772a0-c053-4634-a5e5-999999998781', 'tenant-99', null, null, null, true, true),
    ('218772a0-c053-4634-a5e5-000000008781', 'tenant-00', null, null, null, false, true);

merge into "sys_tenant_system" ("id", "tenant_id", "system_code") values
    ('b3846388-5e61-4b58-8fd8-aaaaaaaa8781', '218772a0-c053-4634-a5e5-111111118781', 'subSys-a'),
    ('b3846388-5e61-4b58-8fd8-bbbbbbbb8781', '218772a0-c053-4634-a5e5-111111118781', 'subSys-b'),
    ('b3846388-5e61-4b58-8fd8-cccccccc8781', '218772a0-c053-4634-a5e5-111111118781', 'subSys-c'),
    ('b3846388-5e61-4b58-8fd8-dddddddd8781', '218772a0-c053-4634-a5e5-222222228781', 'subSys-c'),
    ('b3846388-5e61-4b58-8fd8-eeeeeeee8781', '218772a0-c053-4634-a5e5-333333338781', 'subSys-a'),
    ('b3846388-5e61-4b58-8fd8-ffffffff8781', '218772a0-c053-4634-a5e5-444444448781', 'subSys-a'),
    ('b3846388-5e61-4b58-8fd8-ggggggg_5246', '218772a0-c053-4634-a5e5-555555558781', 'subSys-d'),
    ('b3846388-5e61-4b58-8fd8-hhhhhhh_5246', '218772a0-c053-4634-a5e5-666666668781', 'subSys-d');
