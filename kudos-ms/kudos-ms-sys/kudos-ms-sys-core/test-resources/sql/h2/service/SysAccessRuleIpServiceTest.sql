merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000034', 'svc-tenant-arip-test-1', null, null, 'from SysAccessRuleIpServiceTest', true, false);

merge into "sys_system" ("code", "name", "remark", "active", "built_in")
    values ('svc-system-arip-test-1', 'svc-system-arip-test-1-name', 'from SysAccessRuleIpServiceTest', true, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in")
    values ('svc-subsys-arip-test-1', 'svc-subsys-arip-test-1-name', 'svc-system-arip-test-1', true, 'from SysAccessRuleIpServiceTest', true, false);

merge into "sys_access_rule" ("id", "tenant_id", "system_code", "rule_type_dict_code", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000034', '20000000-0000-0000-0000-000000000034', 'svc-system-arip-test-1', '1', 'from SysAccessRuleIpServiceTest', true, false);

merge into "sys_access_rule_ip" ("id", "ip_start", "ip_end", "ip_type_dict_code", "parent_rule_id", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000034', 2130706433, 2130706433, '1', '20000000-0000-0000-0000-000000000034', 'from SysAccessRuleIpServiceTest', true, false);
