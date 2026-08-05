-- Fixture for the decision algebra.
--
--   staff        holds "staff" role  -> ALLOW sys:user:* (wildcard grant, code-addressed)
--                                    -> DENY  sys:user:delete (an exception carved out of it)
--                                    -> ALLOW msg:template:read only from 10.0.0.0/8 (conditional)
--   auditor      holds "auditor"     -> ALLOW via a resource-id-addressed binding whose sys_resource
--                                       row carries a permission code (the migration path)
--   orphan       holds "orphan" role -> ALLOW via a resource-id binding whose resource has NO code,
--                                       so it contributes nothing and the subject stays denied
--   platform     holds the role code named in kudos.ms.auth.authz.platform-admin-role-codes
--   nobody       holds no role at all

merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('dec10000-0000-0000-0000-000000000001', 'svc-authz-staff-Kq2m', 'tenant-authz-1-Kq2m', 'pwd-Kq2m', '00000000-0000-0000-0000-000000000000', 'from AuthzDecisionApiTest', true, false, 'system', '系统'),
    ('dec10000-0000-0000-0000-000000000002', 'svc-authz-auditor-Kq2m', 'tenant-authz-1-Kq2m', 'pwd-Kq2m', '00000000-0000-0000-0000-000000000000', 'from AuthzDecisionApiTest', true, false, 'system', '系统'),
    ('dec10000-0000-0000-0000-000000000003', 'svc-authz-orphan-Kq2m', 'tenant-authz-1-Kq2m', 'pwd-Kq2m', '00000000-0000-0000-0000-000000000000', 'from AuthzDecisionApiTest', true, false, 'system', '系统'),
    ('dec10000-0000-0000-0000-000000000004', 'svc-authz-platform-Kq2m', 'tenant-authz-1-Kq2m', 'pwd-Kq2m', '00000000-0000-0000-0000-000000000000', 'from AuthzDecisionApiTest', true, false, 'system', '系统'),
    ('dec10000-0000-0000-0000-000000000005', 'svc-authz-nobody-Kq2m', 'tenant-authz-1-Kq2m', 'pwd-Kq2m', '00000000-0000-0000-0000-000000000000', 'from AuthzDecisionApiTest', true, false, 'system', '系统');

merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('dec20000-0000-0000-0000-000000000001', 'svc-authz-staff-Kq2m', 'staff', 'tenant-authz-1-Kq2m', 'ams', 'from AuthzDecisionApiTest', true, false, 'system', '系统'),
    ('dec20000-0000-0000-0000-000000000002', 'svc-authz-auditor-Kq2m', 'auditor', 'tenant-authz-1-Kq2m', 'ams', 'from AuthzDecisionApiTest', true, false, 'system', '系统'),
    ('dec20000-0000-0000-0000-000000000003', 'svc-authz-orphan-Kq2m', 'orphan', 'tenant-authz-1-Kq2m', 'ams', 'from AuthzDecisionApiTest', true, false, 'system', '系统'),
    ('dec20000-0000-0000-0000-000000000004', 'KUDOS_PLATFORM_ADMIN_Kq2m', 'platform admin', 'tenant-authz-1-Kq2m', 'ams', 'from AuthzDecisionApiTest', true, true, 'system', '系统');

merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name") values
    ('dec30000-0000-0000-0000-000000000001', 'dec20000-0000-0000-0000-000000000001', 'dec10000-0000-0000-0000-000000000001', 'system', '系统'),
    ('dec30000-0000-0000-0000-000000000002', 'dec20000-0000-0000-0000-000000000002', 'dec10000-0000-0000-0000-000000000002', 'system', '系统'),
    ('dec30000-0000-0000-0000-000000000003', 'dec20000-0000-0000-0000-000000000003', 'dec10000-0000-0000-0000-000000000003', 'system', '系统'),
    ('dec30000-0000-0000-0000-000000000004', 'dec20000-0000-0000-0000-000000000004', 'dec10000-0000-0000-0000-000000000004', 'system', '系统');

-- One resource registered with a permission code, one deliberately left without.
merge into "sys_resource" ("id", "name", "permission_code", "url", "resource_type_dict_code", "parent_id", "order_num", "sub_system_code", "active", "built_in") values
    ('dec40000-0000-0000-0000-000000000001', 'svc-authz-res-coded-Kq2m', 'audit:log:read', 'GET:/api/admin/audit/log', '2', null, 1, 'ams', true, false),
    ('dec40000-0000-0000-0000-000000000002', 'svc-authz-res-uncoded-Kq2m', null, '/api/admin/legacy/thing', '2', null, 2, 'ams', true, false),
    ('dec40000-0000-0000-0000-000000000003', 'svc-authz-res-subtree-Kq2m', 'ams:widget:manage', '/api/admin/widget/**', '2', null, 3, 'ams', true, false);

merge into "auth_role_resource" ("id", "role_id", "resource_id", "permission_code", "effect", "condition", "create_user_id", "create_user_name") values
    -- staff: a wildcard ALLOW with a single DENY carved out of it, plus a conditional grant.
    ('dec50000-0000-0000-0000-000000000001', 'dec20000-0000-0000-0000-000000000001', null, 'sys:user:*', 'ALLOW', null, 'system', '系统'),
    ('dec50000-0000-0000-0000-000000000002', 'dec20000-0000-0000-0000-000000000001', null, 'sys:user:delete', 'DENY', null, 'system', '系统'),
    ('dec50000-0000-0000-0000-000000000003', 'dec20000-0000-0000-0000-000000000001', null, 'msg:template:read', 'ALLOW', 'ip=10.0.0.0/8', 'system', '系统'),
    -- auditor: resource-id addressed; the code comes from the sys_resource row.
    ('dec50000-0000-0000-0000-000000000004', 'dec20000-0000-0000-0000-000000000002', 'dec40000-0000-0000-0000-000000000001', null, 'ALLOW', null, 'system', '系统'),
    -- orphan: resource-id addressed, but that resource has no permission code registered.
    ('dec50000-0000-0000-0000-000000000005', 'dec20000-0000-0000-0000-000000000003', 'dec40000-0000-0000-0000-000000000002', null, 'ALLOW', null, 'system', '系统');
