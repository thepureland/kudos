-- auth_role: 供时效性授权用例
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('7e3b9a01-0000-0000-0000-0000000000a1', 'tmp-role-1-7e3b9a01', 'tmp-role-1', 'tmp-tenant-7e3b9a01', 'ams', 'temporal', true, false, 'system', '系统'),
    ('7e3b9a01-0000-0000-0000-0000000000a2', 'tmp-role-2-7e3b9a01', 'tmp-role-2', 'tmp-tenant-7e3b9a01', 'ams', 'temporal', true, false, 'system', '系统');

-- user_account
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('7e3b9a01-0000-0000-0000-0000000000b1', 'tmp-user-1-7e3b9a01', 'tmp-tenant-7e3b9a01', 'pwd', '00000000-0000-0000-0000-000000000000', 'temporal', true, false, 'system', '系统'),
    ('7e3b9a01-0000-0000-0000-0000000000b2', 'tmp-user-2-7e3b9a01', 'tmp-tenant-7e3b9a01', 'pwd', '00000000-0000-0000-0000-000000000000', 'temporal', true, false, 'system', '系统');

-- auth_role_user: user1 永久持有 role1（start/end 均 NULL），作为 active 过滤的基线
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name") values
    ('7e3b9a01-0000-0000-0000-0000000000c0', '7e3b9a01-0000-0000-0000-0000000000a1', '7e3b9a01-0000-0000-0000-0000000000b1', 'system', '系统');
