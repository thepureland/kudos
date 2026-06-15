-- Test data: VSysAccessRuleIpServiceTest
-- Seeds parent access rules (tenant-level + platform-level) and their IP children so the view
-- v_sys_access_rule_with_ip returns joined rows for parentId / (systemCode, tenantId) queries.

merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in") values
    ('50000000-0000-0000-0000-000000000901', 'svc-tenant-vsys-test-1', null, null, 'from VSysAccessRuleIpServiceTest', true, false);

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in") values
    ('svc-system-vsys-test-1', 'svc-system-vsys-test-1-name', null, false, 'from VSysAccessRuleIpServiceTest', true, false),
    ('svc-system-vsys-test-plat', 'svc-system-vsys-test-plat-name', null, false, 'from VSysAccessRuleIpServiceTest', true, false);

-- tenant-level parent rule
merge into "sys_access_rule" ("id", "tenant_id", "system_code", "access_rule_type_dict_code", "remark", "active", "built_in") values
    ('50000000-0000-0000-0000-000000000801', '50000000-0000-0000-0000-000000000901', 'svc-system-vsys-test-1', 'RULE_TYPE_WHITELIST', 'tenant parent', true, false);

-- platform-level parent rule (tenant_id IS NULL)
merge into "sys_access_rule" ("id", "tenant_id", "system_code", "access_rule_type_dict_code", "remark", "active", "built_in") values
    ('50000000-0000-0000-0000-000000000802', null, 'svc-system-vsys-test-plat', 'RULE_TYPE_BLACKLIST', 'platform parent', true, false);

-- IP children of the tenant-level parent
merge into "sys_access_rule_ip" ("id", "ip_start", "ip_end", "ip_type_dict_code", "expiration_time", "parent_rule_id", "remark", "active", "built_in") values
    ('50000000-0000-0000-0000-000000000701', 3232235521, 3232235530, 'ipv4', null, '50000000-0000-0000-0000-000000000801', 'ip-a', true, false),
    ('50000000-0000-0000-0000-000000000702', 3232235540, 3232235550, 'ipv4', null, '50000000-0000-0000-0000-000000000801', 'ip-b', true, false);

-- IP child of the platform-level parent
merge into "sys_access_rule_ip" ("id", "ip_start", "ip_end", "ip_type_dict_code", "expiration_time", "parent_rule_id", "remark", "active", "built_in") values
    ('50000000-0000-0000-0000-000000000703', 167772160, 167772415, 'ipv4', null, '50000000-0000-0000-0000-000000000802', 'ip-c', true, false);
