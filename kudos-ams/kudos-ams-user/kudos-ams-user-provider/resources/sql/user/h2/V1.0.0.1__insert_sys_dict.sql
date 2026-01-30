--region DML
merge into "sys_dict" ("id", "module_code", "dict_type", "dict_name", "remark", "active", "built_in")
    values ('68139ed2-dbce-dict-acct-provider0001', 'kudos-user', 'account_provider', '第三方账号提供商', null, true, false);

--endregion DML


