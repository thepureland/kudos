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

-- SoD batchBind 回归防线：a1 < a2 构成互斥对（规范顺序 roleAId=a1, roleBId=a2）；userB(b1) 永久持有较小侧 a1。
-- batchBind 较大侧 a2 给 b1 必须被拒（曾因 searchByRoleIdAndTenant 的 Criteria 别名 bug 漏判 roleBId 侧候选）。
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('7817d37f-0000-0000-0000-0000000000a1', 'svc-role-roleus-soda-BuHDHRZc', 'svc-rol-rol-soda-name-BuHDHRZc', 'svc-tenan-roleu-test-1-BuHDHRZc', 'ams', 'SoD smaller side', true, false, 'system', '系统'),
    ('7817d37f-0000-0000-0000-0000000000a2', 'svc-role-roleus-sodb-BuHDHRZc', 'svc-rol-rol-sodb-name-BuHDHRZc', 'svc-tenan-roleu-test-1-BuHDHRZc', 'ams', 'SoD larger side', true, false, 'system', '系统');

merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('7817d37f-0000-0000-0000-0000000000b1', 'svc-user-roleus-sodu-BuHDHRZc', 'svc-tenan-roleu-test-1-BuHDHRZc', 'encrypted-pwd-sod-BuHDHRZc', '00000000-0000-0000-0000-000000000000', 'SoD user holds a1', true, false, 'system', '系统');

merge into "auth_role_exclusion" ("id", "role_a_id", "role_b_id", "tenant_id", "description", "create_user_id", "create_user_name") values
    ('7817d37f-0000-0000-0000-0000000000d1', '7817d37f-0000-0000-0000-0000000000a1', '7817d37f-0000-0000-0000-0000000000a2', 'svc-tenan-roleu-test-1-BuHDHRZc', 'SoD pair for batchBind regression test', 'system', '系统');

merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name") values
    ('7817d37f-0000-0000-0000-0000000000c1', '7817d37f-0000-0000-0000-0000000000a1', '7817d37f-0000-0000-0000-0000000000b1', 'system', '系统');
