--region DML

merge into "sys_micro_service" ("code", "name", "atomic_service", "context", "remark", "active", "built_in")
    values ('msg', 'msg', true, '/api/msg', null, true, true);

--endregion DML