-- 转授/撤销并发竞态用例:一个可转授的操作者 + 一个受赠者。
-- 一个线程从 boss 的授权转授出去,另一个线程同时撤销 boss 的授权,
-- 看级联是否会漏掉在它的快照之后才落库的子授权(已撤销父授权下的"孤儿"活授权)。

merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('d1610000-0000-0000-0000-000000000001', 'dlgrace-boss-9k2f', 'dlgrace-tenant-9k2f', 'pwd', '00000000-0000-0000-0000-000000000000', 'from DelegationRevokeConcurrencyTest', true, false, 'system', '系统'),
    ('d1610000-0000-0000-0000-000000000002', 'dlgrace-recipient-9k2f', 'dlgrace-tenant-9k2f', 'pwd', '00000000-0000-0000-0000-000000000000', 'from DelegationRevokeConcurrencyTest', true, false, 'system', '系统');

merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "delegable_max", "create_user_id", "create_user_name") values
    ('d1620000-0000-0000-0000-000000000001', 'dlgrace-manager-9k2f', 'manager', 'dlgrace-tenant-9k2f', 'ams', 'delegable', true, false, 2, 'system', '系统');

merge into "auth_role_resource" ("id", "role_id", "resource_id", "permission_code", "effect", "create_user_id", "create_user_name") values
    ('d1640000-0000-0000-0000-000000000001', 'd1620000-0000-0000-0000-000000000001', null, 'dlgrace:order:approve', 'ALLOW', 'system', '系统');

-- boss 持有该角色的直接授权,可再转授两层。这一行是两个线程争夺的对象。
merge into "auth_role_user" ("id", "role_id", "user_id", "delegable_depth", "revoked", "create_user_id", "create_user_name") values
    ('d1650000-0000-0000-0000-000000000001', 'd1620000-0000-0000-0000-000000000001', 'd1610000-0000-0000-0000-000000000001', 2, false, 'system', '系统');
