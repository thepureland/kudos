-- user_account: 每条 id 唯一，使用 auth-group-user-dao-test-* 前缀隔离
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('a1b2c3d4-0000-0000-0000-000000000070', 'group-user-dao-u1', 'tenant-g-dao-test-1', 'encrypted-pwd-1-0xQpE1Zf', '00000000-0000-0000-0000-000000000000', 'from AuthGroupUserDaoTest', true, false, 'system', '系统'),
    ('a1b2c3d4-0000-0000-0000-000000000071', 'authgroup-user-dao-u2', 'tenant-g-dao-test-1', 'encrypted-pwd-2-0xQpE1Zf', '00000000-0000-0000-0000-000000000000', 'from AuthGroupUserDaoTest', true, false, 'system', '系统');

-- auth_group: 每条 id 唯一
merge into "auth_group" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('a1b2c3d4-0000-0000-0000-000000000072', 'group-user-dao-u1', 'g-u-dao-test-tenant-1', 'tenant-g-dao-test-1', 'ams', 'from AuthGroupUserDaoTest', true, false, 'system', '系统'),
    ('a1b2c3d4-0000-0000-0000-000000000073', 'authgroup-user-dao-u2', 'g-u-dao-test-tenant-2', 'tenant-g-dao-test-1', 'ams', 'from AuthGroupUserDaoTest', true, false, 'system', '系统');

-- auth_group_user: 已存在关系供 exists 和查询用例使用
merge into "auth_group_user" ("id", "group_id", "user_id", "create_user_id", "create_user_name") values
    ('a1b2c3d4-0000-0000-0000-000000000074', 'a1b2c3d4-0000-0000-0000-000000000072', 'a1b2c3d4-0000-0000-0000-000000000070', 'system', '系统'),
    ('a1b2c3d4-0000-0000-0000-000000000075', 'a1b2c3d4-0000-0000-0000-000000000072', 'a1b2c3d4-0000-0000-0000-000000000071', 'system', '系统');
