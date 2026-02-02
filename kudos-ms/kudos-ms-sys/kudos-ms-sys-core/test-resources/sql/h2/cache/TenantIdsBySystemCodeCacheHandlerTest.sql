merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('118772a0-c053-4634-a5e5-111111111111', 'tenant-11', null, null, null, true, true),
           ('118772a0-c053-4634-a5e5-222222222222', 'tenant-22', null, null, null, true, true),
           ('118772a0-c053-4634-a5e5-333333333333', 'tenant-33', null, null, null, true, true),
           ('118772a0-c053-4634-a5e5-444444444444', 'tenant-44', null, null, null, true, true),
           ('118772a0-c053-4634-a5e5-555555555555', 'tenant-55', null, null, null, true, true),
           ('118772a0-c053-4634-a5e5-666666666666', 'tenant-66', null, null, null, true, true),
           ('118772a0-c053-4634-a5e5-777777777777', 'tenant-77', null, null, null, true, true),
           ('118772a0-c053-4634-a5e5-888888888888', 'tenant-88', null, null, null, true, true),
           ('118772a0-c053-4634-a5e5-999999999999', 'tenant-99', null, null, null, true, true),
           ('118772a0-c053-4634-a5e5-000000000000', 'tenant-00', null, null, null, false, true);


merge into "sys_tenant_system" ("id", "tenant_id", "system_code")
    values ('b3846388-5e61-4b58-8fd8-aaaaaaaaaaaa', '118772a0-c053-4634-a5e5-111111111111', 'subSys-a'),
           ('b3846388-5e61-4b58-8fd8-bbbbbbbbbbbb', '118772a0-c053-4634-a5e5-111111111111', 'subSys-b'),
           ('b3846388-5e61-4b58-8fd8-cccccccccccc', '118772a0-c053-4634-a5e5-111111111111', 'subSys-c'),
           ('b3846388-5e61-4b58-8fd8-dddddddddddd', '118772a0-c053-4634-a5e5-222222222222', 'subSys-c'),
           ('b3846388-5e61-4b58-8fd8-eeeeeeeeeeee', '118772a0-c053-4634-a5e5-333333333333', 'subSys-a'),
           ('b3846388-5e61-4b58-8fd8-ffffffffffff', '118772a0-c053-4634-a5e5-444444444444', 'subSys-a'),
           ('b3846388-5e61-4b58-8fd8-gggggggggggg', '118772a0-c053-4634-a5e5-555555555555', 'subSys-d'),
           ('b3846388-5e61-4b58-8fd8-hhhhhhhhhhhh', '118772a0-c053-4634-a5e5-666666666666', 'subSys-d');
