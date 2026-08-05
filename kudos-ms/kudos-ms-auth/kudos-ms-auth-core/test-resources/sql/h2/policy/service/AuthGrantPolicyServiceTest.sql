-- Fixture for the shared grant-admission gate. Two mutually exclusive roles (maker / checker) in
-- one tenant, plus a role in a second tenant to exercise the tenant boundary, plus a group that can
-- be used to attempt the classic bypass (get the role via membership instead of a direct grant).

merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('pol10000-0000-0000-0000-000000000001', 'svc-policy-maker-Ph7xR2s', 'tenant-policy-1-Ph7xR2s', 'pwd-Ph7xR2s', '00000000-0000-0000-0000-000000000000', 'from AuthGrantPolicyServiceTest', true, false, 'system', '系统'),
    ('pol10000-0000-0000-0000-000000000002', 'svc-policy-plain-Ph7xR2s', 'tenant-policy-1-Ph7xR2s', 'pwd-Ph7xR2s', '00000000-0000-0000-0000-000000000000', 'from AuthGrantPolicyServiceTest', true, false, 'system', '系统'),
    ('pol10000-0000-0000-0000-000000000003', 'svc-policy-other-tenant-Ph7xR2s', 'tenant-policy-2-Ph7xR2s', 'pwd-Ph7xR2s', '00000000-0000-0000-0000-000000000000', 'from AuthGrantPolicyServiceTest', true, false, 'system', '系统');

merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('pol20000-0000-0000-0000-000000000001', 'svc-policy-maker-Ph7xR2s', 'maker', 'tenant-policy-1-Ph7xR2s', 'ams', 'from AuthGrantPolicyServiceTest', true, false, 'system', '系统'),
    ('pol20000-0000-0000-0000-000000000002', 'svc-policy-checker-Ph7xR2s', 'checker', 'tenant-policy-1-Ph7xR2s', 'ams', 'from AuthGrantPolicyServiceTest', true, false, 'system', '系统'),
    ('pol20000-0000-0000-0000-000000000003', 'svc-policy-t2role-Ph7xR2s', 'other tenant role', 'tenant-policy-2-Ph7xR2s', 'ams', 'from AuthGrantPolicyServiceTest', true, false, 'system', '系统');

-- Separation of duties: nobody may hold maker and checker at the same time.
merge into "auth_role_exclusion" ("id", "role_a_id", "role_b_id", "tenant_id", "description", "create_user_id", "create_user_name") values
    ('pol30000-0000-0000-0000-000000000001', 'pol20000-0000-0000-0000-000000000001', 'pol20000-0000-0000-0000-000000000002', 'tenant-policy-1-Ph7xR2s', 'maker vs checker', 'system', '系统');

-- The maker user already holds the maker role directly.
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name") values
    ('pol40000-0000-0000-0000-000000000001', 'pol20000-0000-0000-0000-000000000001', 'pol10000-0000-0000-0000-000000000001', 'system', '系统');

-- A group that carries the checker role — the bypass route the policy layer has to close.
merge into "auth_group" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('pol50000-0000-0000-0000-000000000001', 'svc-policy-checkers-Ph7xR2s', 'checkers', 'tenant-policy-1-Ph7xR2s', 'ams', 'from AuthGrantPolicyServiceTest', true, false, 'system', '系统'),
    ('pol50000-0000-0000-0000-000000000002', 'svc-policy-empty-Ph7xR2s', 'empty group', 'tenant-policy-1-Ph7xR2s', 'ams', 'from AuthGrantPolicyServiceTest', true, false, 'system', '系统');

merge into "auth_group_role" ("id", "group_id", "role_id", "create_user_id", "create_user_name") values
    ('pol60000-0000-0000-0000-000000000001', 'pol50000-0000-0000-0000-000000000001', 'pol20000-0000-0000-0000-000000000002', 'system', '系统');

-- A registered permission point in the ams subsystem, and one in a different subsystem.
merge into "sys_resource" ("id", "name", "permission_code", "url", "resource_type_dict_code", "parent_id", "order_num", "sub_system_code", "active", "built_in") values
    ('pol70000-0000-0000-0000-000000000001', 'svc-policy-res-ams-Ph7xR2s', 'ams:policy:read', '/policy/read', '2', null, 1, 'ams', true, false),
    ('pol70000-0000-0000-0000-000000000002', 'svc-policy-res-other-Ph7xR2s', 'other:policy:read', '/policy/other', '2', null, 2, 'other-subsys-Ph7xR2s', true, false);
