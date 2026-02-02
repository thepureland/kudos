merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000033', 'svc-tenant-ar-test-1', null, null, 'from SysAccessRuleServiceTest', true, false);

merge into "sys_system" ("code", "name", "remark", "active", "built_in")
    values ('svc-system-ar-test-1', 'svc-system-ar-test-1-name', 'from SysAccessRuleServiceTest', true, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in")
    values ('svc-subsys-ar-test-1', 'svc-subsys-ar-test-1-name', 'svc-system-ar-test-1', true, 'from SysAccessRuleServiceTest', true, false);

merge into "sys_access_rule" ("id", "tenant_id", "system_code", "rule_type_dict_code", "remark", "active", "built_in")
    values ('20000000-0000-0000-0000-000000000033', '20000000-0000-0000-0000-000000000033', 'svc-system-ar-test-1', '0', 'from SysAccessRuleServiceTest', true, false);
