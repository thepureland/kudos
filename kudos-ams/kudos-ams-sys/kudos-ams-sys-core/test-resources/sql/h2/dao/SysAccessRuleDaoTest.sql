-- 测试数据：SysAccessRuleDaoTest
-- 使用唯一前缀 svc-accessrule-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000150', 'svc-tenant-ar-dao-test-1', null, null, 'from SysAccessRuleDaoTest', true, false);

merge into "sys_system" ("code", "name", "remark", "active", "built_in")
    values ('svc-system-ar-dao-test-1', 'svc-system-accessrule-dao-test-1-name', 'from SysAccessRuleDaoTest', true, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in")
    values ('svc-subsys-ar-dao-test-1', 'svc-subsys-accessrule-dao-test-1-name', 'svc-system-ar-dao-test-1', true, 'from SysAccessRuleDaoTest', true, false);

merge into "sys_access_rule" ("id", "tenant_id", "system_code", "rule_type_dict_code", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000151', '40000000-0000-0000-0000-000000000150', 'svc-system-ar-dao-test-1', 'RULE_TYPE_WHITELIST', 'from SysAccessRuleDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000152', '40000000-0000-0000-0000-000000000150', 'svc-system-ar-dao-test-1', 'RULE_TYPE_BLACKLIST', 'from SysAccessRuleDaoTest', true, false);
