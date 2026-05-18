-- 测试数据：SysOutLineServiceTest

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in") values
    ('30000000-0000-0000-0000-000000002001', 'tenant-outline-svc-test-1', null, null, 'from SysOutLineServiceTest', true, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in") values
    ('sys-outline-svc-test', 'sys-outline-svc-test-name', null, false, 'from SysOutLineServiceTest', true, false);

merge into "sys_out_line"
    ("id", "name", "host", "port", "protocol", "system_code", "tenant_id", "remark", "active", "built_in") values
    -- 平台级、启用
    ('30000000-0000-0000-0000-000000001001', 'outline-svc-platform-1', 'example.com', 443, 'https',
     'sys-outline-svc-test', null, 'platform from SysOutLineServiceTest', true, false),
    -- 租户级、启用
    ('30000000-0000-0000-0000-000000001002', 'outline-svc-tenant-1', 'tenant-api.example.com', null, 'any',
     'sys-outline-svc-test', '30000000-0000-0000-0000-000000002001', 'tenant from SysOutLineServiceTest', true, false),
    -- 平台级、未启用（不应出现在 listActive 结果）
    ('30000000-0000-0000-0000-000000001003', 'outline-svc-inactive-1', 'inactive.example.com', null, 'any',
     'sys-outline-svc-test', null, 'inactive from SysOutLineServiceTest', false, false);
