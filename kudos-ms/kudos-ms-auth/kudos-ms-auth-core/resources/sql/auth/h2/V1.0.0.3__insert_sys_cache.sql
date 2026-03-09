--region DML
insert into "sys_cache" ("name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time", "ttl", "remark", "hash") values
    ('AUTH_USER_IDS_BY_TENANT_ID_AND_ROLE_CODE', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户ID列表缓存(by tenantId & roleCode)', false),
    ('AUTH_USER_IDS_BY_TENANT_ID_AND_GROUP_CODE', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户ID列表缓存(by tenantId & groupCode)', false),
    ('AUTH_RESOURCE_IDS_BY_TENANT_ID_AND_ROLE_CODE', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '资源ID列表缓存(by tenantId & roleCode)', false),
    ('AUTH_RESOURCE_IDS_BY_TENANT_ID_AND_GROUP_CODE', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '资源ID列表缓存(by tenantId & groupCode)', false),
    ('AUTH_RESOURCE_IDS_BY_TENANT_ID_AND_USERNAME', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '资源ID列表缓存(by tenantId & username)', false),
    ('AUTH_RESOURCE_IDS_BY_USER_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '资源ID列表缓存(by userId)', false),
    ('AUTH_ROLE_IDS_BY_USER_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '角色ID列表缓存(by userId)', false),
    ('AUTH_GROUP_IDS_BY_USER_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户组ID列表缓存(by userId)', false),
    ( 'AUTH_RESOURCE_IDS_BY_ROLE_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '资源ID列表缓存(by roleId)', false),
    ( 'AUTH_USER_IDS_BY_ROLE_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户ID列表缓存(by roleId)', false),
    ( 'AUTH_USER_IDS_BY_GROUP_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户ID列表缓存(by groupId)', false),
    ('AUTH_GROUP__HASH', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户组Hash缓存', true),
    ('AUTH_ROLE__HASH', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '角色Hash缓存', true);

--endregion DML
