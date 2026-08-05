-- user_account: 每条 id 唯一，使用 auth-roleuser-dao-test-* 前缀隔离
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('42d84639-0000-0000-0000-000000000050', 'user-acc-rol-dao-tes-1-kRWTnffD', 'auth-tena-rol-dao-tes-1-kRWTnffD', 'encrypted-pwd-1-kRWTnffD', '00000000-0000-0000-0000-000000000000', 'from AuthRoleUserDaoTest', true, false, 'system', '系统'),
    ('42d84639-0000-0000-0000-000000000051', 'user-acc-rol-dao-tes-2-kRWTnffD', 'auth-tena-rol-dao-tes-1-kRWTnffD', 'encrypted-pwd-2-kRWTnffD', '00000000-0000-0000-0000-000000000000', 'from AuthRoleUserDaoTest', true, false, 'system', '系统'),
    ('42d84639-0000-0000-0000-000000000056', 'user-acc-rol-dao-tes-3-kRWTnffD', 'auth-tena-rol-dao-tes-1-kRWTnffD', 'encrypted-pwd-3-kRWTnffD', '00000000-0000-0000-0000-000000000000', 'from AuthRoleUserDaoTest', true, false, 'system', '系统');

-- auth_role: 每条 id 唯一
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('42d84639-0000-0000-0000-000000000052', 'auth-role-rol-dao-tes-1-kRWTnffD', 'auth-rol-ro-da-te-1-nam-kRWTnffD', 'auth-tena-rol-dao-tes-1-kRWTnffD', 'ams', 'from AuthRoleUserDaoTest', true, false, 'system', '系统'),
    ('42d84639-0000-0000-0000-000000000053', 'auth-role-rol-dao-tes-2-kRWTnffD', 'auth-rol-ro-da-te-2-nam-kRWTnffD', 'auth-tena-rol-dao-tes-1-kRWTnffD', 'ams', 'from AuthRoleUserDaoTest', true, false, 'system', '系统');

-- auth_role_user 永久授权（start/end 均为 NULL）：role52 -> {user50, user51}
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name") values
    ('42d84639-0000-0000-0000-000000000054', '42d84639-0000-0000-0000-000000000052', '42d84639-0000-0000-0000-000000000050', 'system', '系统'),
    ('42d84639-0000-0000-0000-000000000055', '42d84639-0000-0000-0000-000000000052', '42d84639-0000-0000-0000-000000000051', 'system', '系统');

-- auth_role_user 时间窗授权：用于 isActiveAt / searchExpiredGrants 用例，参照时点取 2025-06-15 12:00:00
-- 已过期 (end_time < now)：role53 -> user50，窗口 2025-01-01 ~ 2025-03-01
-- 未生效 (start_time > now)：role53 -> user51，窗口 2025-12-01 ~ 2026-01-01
-- 当前生效 (start<=now<=end)：role53 -> user52，窗口 2025-06-01 ~ 2025-07-01
merge into "auth_role_user" ("id", "role_id", "user_id", "start_time", "end_time", "create_user_id", "create_user_name") values
    ('42d84639-0000-0000-0000-000000000060', '42d84639-0000-0000-0000-000000000053', '42d84639-0000-0000-0000-000000000050', timestamp '2025-01-01 00:00:00', timestamp '2025-03-01 00:00:00', 'system', '系统'),
    ('42d84639-0000-0000-0000-000000000061', '42d84639-0000-0000-0000-000000000053', '42d84639-0000-0000-0000-000000000051', timestamp '2025-12-01 00:00:00', timestamp '2026-01-01 00:00:00', 'system', '系统'),
    ('42d84639-0000-0000-0000-000000000062', '42d84639-0000-0000-0000-000000000053', '42d84639-0000-0000-0000-000000000056', timestamp '2025-06-01 00:00:00', timestamp '2025-07-01 00:00:00', 'system', '系统');
