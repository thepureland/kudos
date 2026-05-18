-- 测试数据：OutLineBySystemAndTenantCacheTest

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in") values
    ('30000000-0000-0000-0000-000000003001', 'tenant-outline-cache-test', null, null, 'from OutLineBySystemAndTenantCacheTest', true, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in") values
    ('sys-outline-cache-test', 'sys-outline-cache-test-name', null, false, 'from OutLineBySystemAndTenantCacheTest', true, false);

merge into "sys_out_line"
    ("id", "name", "host", "port", "protocol", "system_code", "tenant_id", "remark", "active", "built_in") values
    ('30000000-0000-0000-0000-000000003101', 'cache-platform-1', 'platform.example.com', 443, 'https',
     'sys-outline-cache-test', null, 'cache-platform from test', true, false),
    ('30000000-0000-0000-0000-000000003102', 'cache-tenant-1', 'tenant.example.com', null, 'any',
     'sys-outline-cache-test', '30000000-0000-0000-0000-000000003001', 'cache-tenant from test', true, false),
    ('30000000-0000-0000-0000-000000003103', 'cache-inactive-1', 'inactive.example.com', null, 'any',
     'sys-outline-cache-test', null, 'inactive from test', false, false);
