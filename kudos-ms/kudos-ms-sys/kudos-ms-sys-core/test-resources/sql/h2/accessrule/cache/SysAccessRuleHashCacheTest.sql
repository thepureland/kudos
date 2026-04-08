-- SysAccessRuleHashCacheTest：`SYS_ACCESS_RULE__HASH` 由 Flyway `V1.0.0.20__sys_cache_access_rule_hash.sql` 注册，此处仅造业务表数据。

merge into "sys_access_rule" ("id", "tenant_id", "system_code", "access_rule_type_dict_code", "remark", "active", "built_in") values
    ('a2000000-0000-0000-0000-000000000101', 'tenant-ar-hash-1', 'sys-ar-hash-sub-a', '0', 'from SysAccessRuleHashCacheTest', true, false),
    ('a2000000-0000-0000-0000-000000000102', null, 'sys-ar-hash-platform-x', '1', 'from SysAccessRuleHashCacheTest platform', true, false);
