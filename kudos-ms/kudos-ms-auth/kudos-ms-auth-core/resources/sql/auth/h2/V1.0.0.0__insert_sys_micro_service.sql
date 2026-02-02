--region DML

merge into "sys_micro_service" ("code", "name", "context", "remark", "active", "built_in")
    values ('kudos-auth', 'kudos-auth', '/auth', null, true, true);

--endregion DML