-- 测试数据：SysAccessRuleDaoTest
-- 使用唯一前缀 svc-accessrule-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000150', 'svc-tenant-ar-dao-test-1', null, null, 'from SysAccessRuleDaoTest', true, false);

merge into "sys_portal" ("code", "name", "remark", "active", "built_in")
    values ('svc-portal-ar-dao-test-1', 'svc-portal-accessrule-dao-test-1-name', 'from SysAccessRuleDaoTest', true, false);

merge into "sys_sub_system" ("code", "name", "portal_code", "remark", "active", "built_in")
    values ('svc-subsys-ar-dao-test-1', 'svc-subsys-accessrule-dao-test-1-name', 'svc-portal-ar-dao-test-1', 'from SysAccessRuleDaoTest', true, false);

merge into "sys_access_rule" ("id", "tenant_id", "sub_system_code", "portal_code", "rule_type_dict_code", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000151', '40000000-0000-0000-0000-000000000150', 'svc-subsys-ar-dao-test-1', 'svc-portal-ar-dao-test-1', 'RULE_TYPE_WHITELIST', 'from SysAccessRuleDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000152', '40000000-0000-0000-0000-000000000150', null, 'svc-portal-ar-dao-test-1', 'RULE_TYPE_BLACKLIST', 'from SysAccessRuleDaoTest', true, false);
