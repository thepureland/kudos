-- Fixture for the "why" endpoint. Each user exercises one way a permission can reach (or fail to
-- reach) them, so the explanation's `source` field has something to distinguish.
--
--   staff        direct role with a wildcard ALLOW + a narrow DENY carved out of it,
--                a conditional grant, and a parent role carrying an inherited permission
--   viaGroup     reaches its role only through group membership
--   nobody       holds nothing — the most common denial of all

merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('exp10000-0000-0000-0000-000000000001', 'svc-exp-staff-Zt8', 'tenant-exp-1-Zt8', 'pwd-Zt8', '00000000-0000-0000-0000-000000000000', 'from PermissionExplainServiceTest', true, false, 'system', '系统'),
    ('exp10000-0000-0000-0000-000000000002', 'svc-exp-viagrp-Zt8', 'tenant-exp-1-Zt8', 'pwd-Zt8', '00000000-0000-0000-0000-000000000000', 'from PermissionExplainServiceTest', true, false, 'system', '系统'),
    ('exp10000-0000-0000-0000-000000000003', 'svc-exp-nobody-Zt8', 'tenant-exp-1-Zt8', 'pwd-Zt8', '00000000-0000-0000-0000-000000000000', 'from PermissionExplainServiceTest', true, false, 'system', '系统');

-- "base" is the parent of "staff": nobody is ever assigned it, yet it grants through the chain.
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "parent_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('exp20000-0000-0000-0000-000000000000', 'svc-exp-base-Zt8', 'base', 'tenant-exp-1-Zt8', 'ams', null, 'from PermissionExplainServiceTest', true, false, 'system', '系统'),
    ('exp20000-0000-0000-0000-000000000001', 'svc-exp-staff-Zt8', 'staff', 'tenant-exp-1-Zt8', 'ams', 'exp20000-0000-0000-0000-000000000000', 'from PermissionExplainServiceTest', true, false, 'system', '系统'),
    ('exp20000-0000-0000-0000-000000000002', 'svc-exp-msg-Zt8', 'messaging', 'tenant-exp-1-Zt8', 'ams', null, 'from PermissionExplainServiceTest', true, false, 'system', '系统');

merge into "auth_role_resource" ("id", "role_id", "resource_id", "permission_code", "effect", "condition", "create_user_id", "create_user_name") values
    ('exp30000-0000-0000-0000-000000000001', 'exp20000-0000-0000-0000-000000000001', null, 'sys:user:*', 'ALLOW', null, 'system', '系统'),
    ('exp30000-0000-0000-0000-000000000002', 'exp20000-0000-0000-0000-000000000001', null, 'sys:user:delete', 'DENY', null, 'system', '系统'),
    ('exp30000-0000-0000-0000-000000000003', 'exp20000-0000-0000-0000-000000000001', null, 'ops:console:enter', 'ALLOW', 'ip=10.0.0.0/8', 'system', '系统'),
    ('exp30000-0000-0000-0000-000000000004', 'exp20000-0000-0000-0000-000000000000', null, 'ams:base:view', 'ALLOW', null, 'system', '系统'),
    ('exp30000-0000-0000-0000-000000000005', 'exp20000-0000-0000-0000-000000000002', null, 'msg:template:read', 'ALLOW', null, 'system', '系统');

merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name") values
    ('exp40000-0000-0000-0000-000000000001', 'exp20000-0000-0000-0000-000000000001', 'exp10000-0000-0000-0000-000000000001', 'system', '系统');

merge into "auth_group" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('exp50000-0000-0000-0000-000000000001', 'svc-exp-msgteam-Zt8', 'messaging team', 'tenant-exp-1-Zt8', 'ams', 'from PermissionExplainServiceTest', true, false, 'system', '系统');

merge into "auth_group_role" ("id", "group_id", "role_id", "create_user_id", "create_user_name") values
    ('exp60000-0000-0000-0000-000000000001', 'exp50000-0000-0000-0000-000000000001', 'exp20000-0000-0000-0000-000000000002', 'system', '系统');

merge into "auth_group_user" ("id", "group_id", "user_id", "create_user_id", "create_user_name") values
    ('exp70000-0000-0000-0000-000000000001', 'exp50000-0000-0000-0000-000000000001', 'exp10000-0000-0000-0000-000000000002', 'system', '系统');
