-- Fixture for controlled delegation.
--
--   boss    holds "manager" with delegable_depth = 2 (may pass it on twice more)
--   lead    holds nothing yet — the intended recipient
--   staff   holds nothing yet — the recipient of a second-hop delegation
--   weak    holds "manager" with delegable_depth = 0 (terminal: may not pass it on)
--   viaGrp  holds "manager" only through a group (group-relayed grants never delegate)
--   outsider is in another org, for the scope check
--
-- "manager" carries one permission; "director" carries that one plus another, so the
-- permission-containment check has something to fail on.

-- The scope check compares user_account.org_id against the ids frozen in a grant's scope snapshot,
-- so the org rows themselves are not needed here.
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "org_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('dlg10000-0000-0000-0000-000000000001', 'svc-dlg-boss-Wn4t', 'tenant-dlg-1-Wn4t', 'pwd-Wn4t', '00000000-0000-0000-0000-000000000000', 'dlg00000-0000-0000-0000-00000000000a', 'from AuthRoleDelegationTest', true, false, 'system', '系统'),
    ('dlg10000-0000-0000-0000-000000000002', 'svc-dlg-lead-Wn4t', 'tenant-dlg-1-Wn4t', 'pwd-Wn4t', '00000000-0000-0000-0000-000000000000', 'dlg00000-0000-0000-0000-00000000000a', 'from AuthRoleDelegationTest', true, false, 'system', '系统'),
    ('dlg10000-0000-0000-0000-000000000003', 'svc-dlg-staff-Wn4t', 'tenant-dlg-1-Wn4t', 'pwd-Wn4t', '00000000-0000-0000-0000-000000000000', 'dlg00000-0000-0000-0000-00000000000a', 'from AuthRoleDelegationTest', true, false, 'system', '系统'),
    ('dlg10000-0000-0000-0000-000000000004', 'svc-dlg-weak-Wn4t', 'tenant-dlg-1-Wn4t', 'pwd-Wn4t', '00000000-0000-0000-0000-000000000000', 'dlg00000-0000-0000-0000-00000000000a', 'from AuthRoleDelegationTest', true, false, 'system', '系统'),
    ('dlg10000-0000-0000-0000-000000000005', 'svc-dlg-viagrp-Wn4t', 'tenant-dlg-1-Wn4t', 'pwd-Wn4t', '00000000-0000-0000-0000-000000000000', 'dlg00000-0000-0000-0000-00000000000a', 'from AuthRoleDelegationTest', true, false, 'system', '系统'),
    ('dlg10000-0000-0000-0000-000000000006', 'svc-dlg-outsider-Wn4t', 'tenant-dlg-1-Wn4t', 'pwd-Wn4t', '00000000-0000-0000-0000-000000000000', 'dlg00000-0000-0000-0000-00000000000b', 'from AuthRoleDelegationTest', true, false, 'system', '系统');

merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "delegable_max", "create_user_id", "create_user_name") values
    ('dlg20000-0000-0000-0000-000000000001', 'svc-dlg-manager-Wn4t', 'manager', 'tenant-dlg-1-Wn4t', 'ams', 'delegable twice', true, false, 2, 'system', '系统'),
    ('dlg20000-0000-0000-0000-000000000002', 'svc-dlg-director-Wn4t', 'director', 'tenant-dlg-1-Wn4t', 'ams', 'strictly more powerful', true, false, 2, 'system', '系统'),
    ('dlg20000-0000-0000-0000-000000000003', 'svc-dlg-sealed-Wn4t', 'sealed', 'tenant-dlg-1-Wn4t', 'ams', 'never delegable', true, false, 0, 'system', '系统'),
    -- Carries a DENY. Held by boss, it removes ams:order:close from what boss can actually exercise,
    -- so boss holding "director" is no longer the same as boss being able to do everything it grants.
    ('dlg20000-0000-0000-0000-000000000004', 'svc-dlg-restricted-Wn4t', 'restricted', 'tenant-dlg-1-Wn4t', 'ams', 'carries a DENY', true, false, 2, 'system', '系统');

merge into "sys_resource" ("id", "name", "permission_code", "url", "resource_type_dict_code", "parent_id", "order_num", "sub_system_code", "active", "built_in") values
    ('dlg30000-0000-0000-0000-000000000001', 'svc-dlg-res-approve-Wn4t', 'ams:order:approve', '/api/ams/order/approve', '2', null, 1, 'ams', true, false),
    ('dlg30000-0000-0000-0000-000000000002', 'svc-dlg-res-close-Wn4t', 'ams:order:close', '/api/ams/order/close', '2', null, 2, 'ams', true, false);

merge into "auth_role_resource" ("id", "role_id", "resource_id", "permission_code", "effect", "create_user_id", "create_user_name") values
    ('dlg40000-0000-0000-0000-000000000001', 'dlg20000-0000-0000-0000-000000000001', null, 'ams:order:approve', 'ALLOW', 'system', '系统'),
    ('dlg40000-0000-0000-0000-000000000002', 'dlg20000-0000-0000-0000-000000000002', null, 'ams:order:approve', 'ALLOW', 'system', '系统'),
    ('dlg40000-0000-0000-0000-000000000003', 'dlg20000-0000-0000-0000-000000000002', null, 'ams:order:close', 'ALLOW', 'system', '系统'),
    ('dlg40000-0000-0000-0000-000000000004', 'dlg20000-0000-0000-0000-000000000003', null, 'ams:order:approve', 'ALLOW', 'system', '系统'),
    ('dlg40000-0000-0000-0000-000000000005', 'dlg20000-0000-0000-0000-000000000004', null, 'ams:order:close', 'DENY', 'system', '系统');

merge into "auth_role_user" ("id", "role_id", "user_id", "delegable_depth", "revoked", "create_user_id", "create_user_name") values
    -- boss may pass "manager" on twice more.
    ('dlg50000-0000-0000-0000-000000000001', 'dlg20000-0000-0000-0000-000000000001', 'dlg10000-0000-0000-0000-000000000001', 2, false, 'system', '系统'),
    -- weak holds the same role but terminally.
    ('dlg50000-0000-0000-0000-000000000002', 'dlg20000-0000-0000-0000-000000000001', 'dlg10000-0000-0000-0000-000000000004', 0, false, 'system', '系统'),
    -- boss also holds "sealed", whose ceiling forbids delegation regardless of the grant.
    ('dlg50000-0000-0000-0000-000000000003', 'dlg20000-0000-0000-0000-000000000003', 'dlg10000-0000-0000-0000-000000000001', 2, false, 'system', '系统'),
    -- boss holds "director" directly and delegably, but also the DENY-carrying "restricted" role.
    -- Holding a role and being able to exercise all of it are then two different things.
    ('dlg50000-0000-0000-0000-000000000004', 'dlg20000-0000-0000-0000-000000000002', 'dlg10000-0000-0000-0000-000000000001', 2, false, 'system', '系统'),
    ('dlg50000-0000-0000-0000-000000000005', 'dlg20000-0000-0000-0000-000000000004', 'dlg10000-0000-0000-0000-000000000001', 0, false, 'system', '系统');

-- viaGrp reaches "manager" only through a group; group-relayed roles carry no delegation authority.
merge into "auth_group" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('dlg60000-0000-0000-0000-000000000001', 'svc-dlg-managers-Wn4t', 'managers', 'tenant-dlg-1-Wn4t', 'ams', 'from AuthRoleDelegationTest', true, false, 'system', '系统');

merge into "auth_group_role" ("id", "group_id", "role_id", "create_user_id", "create_user_name") values
    ('dlg70000-0000-0000-0000-000000000001', 'dlg60000-0000-0000-0000-000000000001', 'dlg20000-0000-0000-0000-000000000001', 'system', '系统');

merge into "auth_group_user" ("id", "group_id", "user_id", "create_user_id", "create_user_name") values
    ('dlg80000-0000-0000-0000-000000000001', 'dlg60000-0000-0000-0000-000000000001', 'dlg10000-0000-0000-0000-000000000005', 'system', '系统');
