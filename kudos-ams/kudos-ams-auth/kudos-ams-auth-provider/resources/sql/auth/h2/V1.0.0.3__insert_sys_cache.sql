--region DML
insert into "sys_cache" ("id", "name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time",
                         "ttl", "remark", "active", "built_in")
values ('a1a1a1a1-1111-1111-1111-111111111103', 'AUTH_ROLE_BY_ID', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '角色缓存(by id)', true, true),
       ('a1a1a1a1-1111-1111-1111-111111111105', 'AUTH_ROLE_ID_BY_TENANT_ID_AND_ROLE_CODE', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '角色ID缓存(by tenantId & roleCode)', true, true),
       ('a1a1a1a1-1111-1111-1111-111111111108', 'AUTH_USER_IDS_BY_TENANT_ID_AND_ROLE_CODE', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '用户ID列表缓存(by tenantId & roleCode)', true, true),
       ('a1a1a1a1-1111-1111-1111-111111111109', 'AUTH_RESOURCE_IDS_BY_ROLE_CODE', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '资源ID列表缓存(by tenantId & roleCode)', true, true),
       ('a1a1a1a1-1111-1111-1111-111111111110', 'AUTH_RESOURCE_IDS_BY_TENANT_ID_AND_USERNAME', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '资源ID列表缓存(by tenantId & username)', true, true),
       ('a1a1a1a1-1111-1111-1111-111111111111', 'AUTH_RESOURCE_IDS_BY_USER_ID', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '资源ID列表缓存(by userId)', true, true),
       ('a1a1a1a1-1111-1111-1111-111111111112', 'AUTH_ROLE_IDS_BY_USER_ID', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '角色ID列表缓存(by userId)', true, true),
       ('a1a1a1a1-1111-1111-1111-111111111114', 'AUTH_RESOURCE_IDS_BY_ROLE_ID', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '资源ID列表缓存(by roleId)', true, true),
       ('a1a1a1a1-1111-1111-1111-111111111115', 'AUTH_USER_IDS_BY_ROLE_ID', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '用户ID列表缓存(by roleId)', true, true);

--endregion DML
