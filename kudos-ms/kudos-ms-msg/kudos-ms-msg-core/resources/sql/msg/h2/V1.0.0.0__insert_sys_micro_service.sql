--region DML

merge into "sys_micro_service" ("code", "name", "context", "remark", "active", "built_in")
    values ('kudos-msg', 'kudos-msg', '/msg', null, true, true);

--endregion DML