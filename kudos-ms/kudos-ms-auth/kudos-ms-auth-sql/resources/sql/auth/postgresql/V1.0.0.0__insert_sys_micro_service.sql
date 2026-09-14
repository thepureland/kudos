--region DML

insert into "sys_micro_service" ("code", "name", "atomic_service", "context", "remark", "active", "built_in")
    values ('auth', 'auth', true, '/api/auth', null, true, true)
    on conflict ("code") do nothing;

--endregion DML
