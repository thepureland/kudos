-- user_account: 每条 id 唯一，使用 svc-roleuser-test-* 前缀隔离
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('7817d37f-0000-0000-0000-000000000040', 'svc-user-roleus-test-1-BuHDHRZc', 'svc-tenan-roleu-test-1-BuHDHRZc', 'encrypted-pwd-1-BuHDHRZc', '00000000-0000-0000-0000-000000000000', 'from AuthRoleUserServiceTest', true, false, 'system', '系统'),
    ('7817d37f-0000-0000-0000-000000000041', 'svc-user-roleus-test-2-BuHDHRZc', 'svc-tenan-roleu-test-1-BuHDHRZc', 'encrypted-pwd-2-BuHDHRZc', '00000000-0000-0000-0000-000000000000', 'from AuthRoleUserServiceTest', true, false, 'system', '系统'),
    ('7817d37f-0000-0000-0000-000000000042', 'svc-user-roleus-test-3-BuHDHRZc', 'svc-tenan-roleu-test-1-BuHDHRZc', 'encrypted-pwd-3-BuHDHRZc', '00000000-0000-0000-0000-000000000000', 'from AuthRoleUserServiceTest', true, false, 'system', '系统');

-- auth_role: 每条 id 唯一
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('7817d37f-0000-0000-0000-000000000043', 'svc-role-roleus-test-1-BuHDHRZc', 'svc-rol-rol-tes-1-name-BuHDHRZc', 'svc-tenan-roleu-test-1-BuHDHRZc', 'ams', 'from AuthRoleUserServiceTest', true, false, 'system', '系统'),
    ('7817d37f-0000-0000-0000-000000000044', 'svc-role-roleus-test-2-BuHDHRZc', 'svc-rol-rol-tes-2-name-BuHDHRZc', 'svc-tenan-roleu-test-1-BuHDHRZc', 'ams', 'from AuthRoleUserServiceTest', true, false, 'system', '系统');

-- auth_role_user: 已存在关系供 exists 和 unbind 用例使用
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name") values
    ('7817d37f-0000-0000-0000-000000000045', '7817d37f-0000-0000-0000-000000000043', '7817d37f-0000-0000-0000-000000000040', 'system', '系统'),
    ('7817d37f-0000-0000-0000-000000000046', '7817d37f-0000-0000-0000-000000000043', '7817d37f-0000-0000-0000-000000000041', 'system', '系统');
