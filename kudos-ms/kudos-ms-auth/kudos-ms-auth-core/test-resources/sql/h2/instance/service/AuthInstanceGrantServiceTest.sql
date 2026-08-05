-- Fixture for instance-level sharing.
--
--   owner   holds a role granting doc:read on documents generally
--   guest   holds no role at all — everything they can do must come from a share
--   blocked holds the same role as owner, so denying them one particular document exercises the
--           case roles cannot express at all

merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('ins10000-0000-0000-0000-000000000001', 'svc-ins-owner-Cq3', 'tenant-ins-1-Cq3', 'pwd-Cq3', '00000000-0000-0000-0000-000000000000', 'from AuthInstanceGrantServiceTest', true, false, 'system', '系统'),
    ('ins10000-0000-0000-0000-000000000002', 'svc-ins-guest-Cq3', 'tenant-ins-1-Cq3', 'pwd-Cq3', '00000000-0000-0000-0000-000000000000', 'from AuthInstanceGrantServiceTest', true, false, 'system', '系统'),
    ('ins10000-0000-0000-0000-000000000003', 'svc-ins-blocked-Cq3', 'tenant-ins-1-Cq3', 'pwd-Cq3', '00000000-0000-0000-0000-000000000000', 'from AuthInstanceGrantServiceTest', true, false, 'system', '系统');

merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('ins20000-0000-0000-0000-000000000001', 'svc-ins-reader-Cq3', 'reader', 'tenant-ins-1-Cq3', 'ams', 'from AuthInstanceGrantServiceTest', true, false, 'system', '系统');

merge into "auth_role_resource" ("id", "role_id", "resource_id", "permission_code", "effect", "create_user_id", "create_user_name") values
    ('ins30000-0000-0000-0000-000000000001', 'ins20000-0000-0000-0000-000000000001', null, 'doc:read', 'ALLOW', 'system', '系统');

merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name") values
    ('ins40000-0000-0000-0000-000000000001', 'ins20000-0000-0000-0000-000000000001', 'ins10000-0000-0000-0000-000000000001', 'system', '系统'),
    ('ins40000-0000-0000-0000-000000000002', 'ins20000-0000-0000-0000-000000000001', 'ins10000-0000-0000-0000-000000000003', 'system', '系统');
