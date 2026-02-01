-- 测试数据：SysAccessRuleIpDaoTest
-- 使用唯一前缀 svc-accessruleip-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000110', 'svc-tenant-arip-dao-test-1', null, null, 'from SysAccessRuleIpDaoTest', true, false);

merge into "sys_portal" ("code", "name", "remark", "active", "built_in")
    values ('svc-portal-arip-dao-test-1', 'svc-portal-accessruleip-dao-test-1-name', 'from SysAccessRuleIpDaoTest', true, false);

merge into "sys_sub_system" ("code", "name", "portal_code", "remark", "active", "built_in")
    values ('svc-subsys-arip-dao-test-1', 'svc-subsys-accessruleip-dao-test-1-name', 'svc-portal-arip-dao-test-1', 'from SysAccessRuleIpDaoTest', true, false);

merge into "sys_access_rule" ("id", "tenant_id", "sub_system_code", "portal_code", "rule_type_dict_code", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000111', '40000000-0000-0000-0000-000000000110', 'svc-subsys-arip-dao-test-1', 'svc-portal-arip-dao-test-1', 'RULE_TYPE_WHITELIST', 'from SysAccessRuleIpDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000112', '40000000-0000-0000-0000-000000000110', null, 'svc-portal-arip-dao-test-1', 'RULE_TYPE_BLACKLIST', 'from SysAccessRuleIpDaoTest', false, false);

merge into "sys_access_rule_ip" ("id", "ip_start", "ip_end", "ip_type_dict_code", "expiration_time", "parent_rule_id", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000113', 192168001001, 192168001001, 'IP_TYPE_IPV4', null, '40000000-0000-0000-0000-000000000111', 'from SysAccessRuleIpDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000114', 192168001002, 192168001002, 'IP_TYPE_IPV4', null, '40000000-0000-0000-0000-000000000111', 'from SysAccessRuleIpDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000115', 192168001003, 192168001003, 'IP_TYPE_IPV4', null, '40000000-0000-0000-0000-000000000111', 'from SysAccessRuleIpDaoTest', false, false);
