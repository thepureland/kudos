-- user_account: 每条 id 唯一，供资源缓存同步用例使用
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('3248fb0d-0000-0000-0000-000000000055', 'svc-user-roleres-test-1-fHos3r3I', 'svc-tenan-roler-test-1-fHos3r3I', 'encrypted-pwd-1-fHos3r3I', '00000000-0000-0000-0000-000000000000', 'from AuthRoleResourceServiceTest', true, false, 'system', '系统');

-- auth_role: 每条 id 唯一，使用 svc-roleres-test-* 前缀隔离
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('3248fb0d-0000-0000-0000-000000000050', 'svc-role-roleres-test-1-fHos3r3I', 'svc-rol-rol-tes-1-name-fHos3r3I', 'svc-tenan-roler-test-1-fHos3r3I', 'ams', 'from AuthRoleResourceServiceTest', true, false, 'system', '系统'),
    ('3248fb0d-0000-0000-0000-000000000051', 'svc-role-roleres-test-2-fHos3r3I', 'svc-rol-rol-tes-2-name-fHos3r3I', 'svc-tenan-roler-test-1-fHos3r3I', 'ams', 'from AuthRoleResourceServiceTest', true, false, 'system', '系统');

-- auth_role_resource: 已存在关系供 exists 和 unbind 用例使用
merge into "auth_role_resource" ("id", "role_id", "resource_id", "create_user_id", "create_user_name") values
    ('3248fb0d-0000-0000-0000-000000000052', '3248fb0d-0000-0000-0000-000000000050', '3248fb0d-0000-0000-0000-000000000056', 'system', '系统'),
    ('3248fb0d-0000-0000-0000-000000000053', '3248fb0d-0000-0000-0000-000000000050', '3248fb0d-0000-0000-0000-000000000057', 'system', '系统');

-- auth_role_user: 供资源缓存同步用例使用
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name") values
    ('3248fb0d-0000-0000-0000-000000000054', '3248fb0d-0000-0000-0000-000000000050', '3248fb0d-0000-0000-0000-000000000055', 'system', '系统');
