--region DML
merge into "sys_dict" ("id", "module_code", "dict_type", "dict_name", "remark", "active", "built_in")
    values
    ('68139ed2-dict-user-acct-type00000000', 'kudos-user', 'account_type', '账号类型', null, true, true),
    ('68139ed2-dict-user-acct-status000000', 'kudos-user', 'account_status', '账号状态', null, true, true),
    ('68139ed2-dict-user-acct-provider0000', 'kudos-user', 'account_provider', '第三方账号提供商', null, true, true),
    ('68139ed2-dict-user-0org-type00000000', 'kudos-user', 'org_type', '机构类型', null, true, true);

--endregion DML


