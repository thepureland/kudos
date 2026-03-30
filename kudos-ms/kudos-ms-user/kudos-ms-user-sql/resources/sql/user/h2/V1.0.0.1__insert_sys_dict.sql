--region DML

merge into "sys_dict" ("id", "atomic_service_code", "dict_type", "dict_name", "remark", "active", "built_in") values
    ('68139ed2-dict-user-acct-type00000000', 'user', 'account_type', 'account_type', '账号类型', true, true),
    ('68139ed2-dict-user-acct-status000000', 'user', 'account_status', 'account_status', '账号状态', true, true),
    ('68139ed2-dict-user-acct-provider0000', 'user', 'account_provider', 'account_provider', '第三方账号提供商', true, true),
    ('68139ed2-dict-user-0org-type00000000', 'user', 'org_type', 'org_type', '机构类型', true, true),
    ('68139ed2-dict-user-0000-contactway00', 'user', 'contact_way', 'contact_way', '联系方式', true, true),
    ('68139ed2-dict-user-0000-contactwayst', 'user', 'contact_way_status', 'contact_way_status', '联系方式状态', true, true);

--endregion DML


