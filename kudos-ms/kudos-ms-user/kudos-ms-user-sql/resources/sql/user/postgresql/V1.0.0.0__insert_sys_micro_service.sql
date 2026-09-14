--region DML

insert into "sys_micro_service" ("code", "name", "atomic_service", "context", "remark", "active", "built_in")
    values ('user', 'user', true, '/api/user', null, true, true)
    on conflict ("code") do nothing;

--endregion DML
