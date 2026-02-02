-- 测试数据：SysAccessRuleIpDaoTest
-- sys_access_rule / sys_access_rule_ip 每条 id 唯一，与用例中的 tenantId/systemCode 及 count >= 2 对应

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in") values
    ('40000000-0000-0000-0000-000000002666', 'svc-tenant-arip-dao-test-1', null, null, 'from SysAccessRuleIpDaoTest', true, false);

merge into "sys_system" ("code", "name", "remark", "active", "built_in") values
    ('svc-system-arip-dao-test-1_3790', 'svc-system-accessruleip-dao-test-1-name', 'from SysAccessRuleIpDaoTest', true, false),
    ('svc-system-arip-dao-test-2', 'svc-system-accessruleip-dao-test-2-name', 'from SysAccessRuleIpDaoTest', true, false);

merge into "sys_access_rule" ("id", "tenant_id", "system_code", "rule_type_dict_code", "remark", "active", "built_in") values
    ('40000000-0000-0000-0000-000000002666', '40000000-0000-0000-0000-000000002666', 'svc-system-arip-dao-test-1_3790', 'RULE_TYPE_WHITELIST', 'from SysAccessRuleIpDaoTest', true, false),
    ('40000000-0000-0000-0000-000000002667', '40000000-0000-0000-0000-000000002666', 'svc-system-arip-dao-test-2', 'RULE_TYPE_BLACKLIST', 'from SysAccessRuleIpDaoTest', false, false);

merge into "sys_access_rule_ip" ("id", "ip_start", "ip_end", "ip_type_dict_code", "expiration_time", "parent_rule_id", "remark", "active", "built_in") values
    ('40000000-0000-0000-0000-000000002666', 192168001001, 192168001001, 'IP_TYPE_IPV4', null, '40000000-0000-0000-0000-000000002666', 'from SysAccessRuleIpDaoTest', true, false),
    ('40000000-0000-0000-0000-000000002667', 192168001002, 192168001002, 'IP_TYPE_IPV4', null, '40000000-0000-0000-0000-000000002666', 'from SysAccessRuleIpDaoTest', true, false),
    ('40000000-0000-0000-0000-000000002668', 192168001003, 192168001003, 'IP_TYPE_IPV4', null, '40000000-0000-0000-0000-000000002666', 'from SysAccessRuleIpDaoTest', false, false);
