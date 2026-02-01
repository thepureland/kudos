--region DML

merge into "sys_micro_service" ("code", "name", "context", "remark", "active", "built_in")
    values ('kudos-user', 'kudos-user', '/user', null, true, true);

--endregion DML