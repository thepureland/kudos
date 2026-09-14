--region DML
insert into "sys_cache" ("name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time", "ttl", "remark", "built_in", "hash") values
    ('AUTH_USER_IDS_BY_TENANT_ID_AND_ROLE_CODE', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户ID列表缓存(by tenantId & roleCode)', true, false),
    ('AUTH_USER_IDS_BY_TENANT_ID_AND_GROUP_CODE', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户ID列表缓存(by tenantId & groupCode)', true, false),
    ('AUTH_RESOURCE_IDS_BY_TENANT_ID_AND_ROLE_CODE', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '资源ID列表缓存(by tenantId & roleCode)', true, false),
    ('AUTH_RESOURCE_IDS_BY_TENANT_ID_AND_GROUP_CODE', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '资源ID列表缓存(by tenantId & groupCode)', true, false),
    ('AUTH_RESOURCE_IDS_BY_TENANT_ID_AND_USERNAME', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '资源ID列表缓存(by tenantId & username)', true, false),
    ('AUTH_RESOURCE_IDS_BY_USER_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '资源ID列表缓存(by userId)', true, false),
    ('AUTH_ROLE_IDS_BY_USER_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '角色ID列表缓存(by userId)', true, false),
    ('AUTH_GROUP_IDS_BY_USER_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户组ID列表缓存(by userId)', true, false),
    ( 'AUTH_RESOURCE_IDS_BY_ROLE_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '资源ID列表缓存(by roleId)', true, false),
    ( 'AUTH_USER_IDS_BY_ROLE_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户ID列表缓存(by roleId)', true, false),
    ( 'AUTH_USER_IDS_BY_GROUP_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户ID列表缓存(by groupId)', true, false),
    ('AUTH_GROUP__HASH', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户组Hash缓存', true, true),
    ('AUTH_ROLE__HASH', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '角色Hash缓存', true, true),
    -- Backs the authorization decision point. Every enforcement check reads it, so it must be
    -- resolved once per principal and never per question.
    ('AUTH_PERMISSION_GRANTS_BY_USER_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '权限授权项缓存(by userId)', true, false),
    -- Backs row-level filtering, which runs on the *query* path: without this, every filtered select
    -- by a restricted user re-runs the org subtree expansion and the CUSTOM grant lookup.
    -- The only bounded TTL in this file, and deliberately so: every other cache here is invalidated
    -- exhaustively because every input is a table this module owns. This one also depends on
    -- application-contributed IScopeDimensionProvider data, whose writes it cannot observe — the TTL
    -- is what bounds staleness for the inputs the listeners structurally cannot cover.
    ('AUTH_DATA_SCOPE_BY_USER_ID', 'auth', 'LOCAL_REMOTE', true, true, 300, '数据范围缓存(by userId)', true, false),
    -- Compared on every request by any PEP that validates token freshness, so it must be a cache hit.
    -- Derived from AUTH_PERMISSION_GRANTS_BY_USER_ID, and invalidated by the same events plus the
    -- administrative epoch bump.
    ('AUTH_PERMISSION_VERSION_BY_USER_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '权限版本号缓存(by userId)', true, false);

--endregion DML
