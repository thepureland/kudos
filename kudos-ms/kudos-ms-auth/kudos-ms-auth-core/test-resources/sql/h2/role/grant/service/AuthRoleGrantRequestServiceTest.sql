-- Fixtures for AuthRoleGrantRequestService: 1 tenant, 2 users, 2 roles (one approval-required),
-- and one pre-existing role-user binding to exercise the "already holds the role" guard.

merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('grant000-0000-0000-0000-000000000001', 'svc-user-grant-1', 'svc-tenant-grant-1', 'pwd-1', '00000000-0000-0000-0000-000000000000', 'from AuthRoleGrantRequestServiceTest', true, false, 'system', '系统'),
    ('grant000-0000-0000-0000-000000000002', 'svc-user-grant-2', 'svc-tenant-grant-1', 'pwd-2', '00000000-0000-0000-0000-000000000000', 'from AuthRoleGrantRequestServiceTest', true, false, 'system', '系统'),
    -- An eligible approver: holds the approval-required role with delegation allowance, i.e. could
    -- have granted it directly. And a bystander who holds nothing.
    ('grant000-0000-0000-0000-000000000003', 'svc-user-grant-approver', 'svc-tenant-grant-1', 'pwd-3', '00000000-0000-0000-0000-000000000000', 'from AuthRoleGrantRequestServiceTest', true, false, 'system', '系统'),
    ('grant000-0000-0000-0000-000000000004', 'svc-user-grant-bystander', 'svc-tenant-grant-1', 'pwd-4', '00000000-0000-0000-0000-000000000000', 'from AuthRoleGrantRequestServiceTest', true, false, 'system', '系统');

merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "approval_required", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('grant000-0000-0000-0000-000000000010', 'svc-role-grant-normal',   'grant-normal',   'svc-tenant-grant-1', 'ams', false, 'from AuthRoleGrantRequestServiceTest', true, false, 'system', '系统'),
    ('grant000-0000-0000-0000-000000000011', 'svc-role-grant-approval', 'grant-approval', 'svc-tenant-grant-1', 'ams', true,  'from AuthRoleGrantRequestServiceTest', true, false, 'system', '系统');

-- The approval-required role must be delegable for the approver's own grant to entitle them.
update "auth_role" set "delegable_max" = 1 where "id" = 'grant000-0000-0000-0000-000000000011';

-- user-1 already holds the normal role (for the "already holds" guard).
merge into "auth_role_user" ("id", "role_id", "user_id", "delegable_depth", "create_user_id", "create_user_name") values
    ('grant000-0000-0000-0000-000000000020', 'grant000-0000-0000-0000-000000000010', 'grant000-0000-0000-0000-000000000001', 0, 'system', '系统'),
    -- The approver's own delegable grant of the approval-required role is what entitles them to decide.
    ('grant000-0000-0000-0000-000000000021', 'grant000-0000-0000-0000-000000000011', 'grant000-0000-0000-0000-000000000003', 1, 'system', '系统');
