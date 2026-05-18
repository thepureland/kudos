-- 测试数据：SysOutLineDaoTest

merge into "sys_out_line" ("id", "name", "host", "port", "protocol", "system_code", "tenant_id", "remark", "active", "built_in") values
    ('30000000-0000-0000-0000-000000000001', 'outline-dao-1', 'example.com', 443, 'https', 'sys-outline-dao', null, 'from SysOutLineDaoTest', true, false),
    ('30000000-0000-0000-0000-000000000002', 'outline-dao-2', 'api.example.com', null, 'any', 'sys-outline-dao', null, null, false, false);
