-- user_account: 每条 id 唯一，供资源缓存同步用例使用
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('3248fb0d-0000-0000-0000-000000000055', 'svc-user-roleres-test-1-fHos3r3I', 'svc-tenan-roler-test-1-fHos3r3I', 'encrypted-pwd-1-fHos3r3I', '00000000-0000-0000-0000-000000000000', 'from AuthRoleResourceServiceTest', true, false, 'system', '系统');

-- auth_role: 每条 id 唯一，使用 svc-roleres-test-* 前缀隔离
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('3248fb0d-0000-0000-0000-000000000050', 'svc-role-roleres-test-1-fHos3r3I', 'svc-rol-rol-tes-1-name-fHos3r3I', 'svc-tenan-roler-test-1-fHos3r3I', 'ams', 'from AuthRoleResourceServiceTest', true, false, 'system', '系统'),
    ('3248fb0d-0000-0000-0000-000000000051', 'svc-role-roleres-test-2-fHos3r3I', 'svc-rol-rol-tes-2-name-fHos3r3I', 'svc-tenan-roler-test-1-fHos3r3I', 'ams', 'from AuthRoleResourceServiceTest', true, false, 'system', '系统');

-- sys_resource: 被绑定的资源必须真实存在且与角色同子系统（IAuthGrantPolicyService 会校验），
-- 否则授权表里会留下悬空的 resource_id。角色用的是 ams 子系统。
merge into "sys_resource" ("id", "name", "permission_code", "url", "resource_type_dict_code", "parent_id", "order_num", "sub_system_code", "active", "built_in") values
    ('3248fb0d-0000-0000-0000-000000000056', 'svc-res-roleres-test-1-fHos3r3I', 'ams:roleres:read', '/roleres/1', '2', null, 1, 'ams', true, false),
    ('3248fb0d-0000-0000-0000-000000000057', 'svc-res-roleres-test-2-fHos3r3I', 'ams:roleres:write', '/roleres/2', '2', null, 2, 'ams', true, false),
    ('30000000-0000-0000-0000-000000000058', 'svc-res-roleres-test-3-fHos3r3I', 'ams:roleres:delete', '/roleres/3', '2', null, 3, 'ams', true, false);

-- auth_role_resource: 已存在关系供 exists 和 unbind 用例使用
merge into "auth_role_resource" ("id", "role_id", "resource_id", "create_user_id", "create_user_name") values
    ('3248fb0d-0000-0000-0000-000000000052', '3248fb0d-0000-0000-0000-000000000050', '3248fb0d-0000-0000-0000-000000000056', 'system', '系统'),
    ('3248fb0d-0000-0000-0000-000000000053', '3248fb0d-0000-0000-0000-000000000050', '3248fb0d-0000-0000-0000-000000000057', 'system', '系统');

-- auth_role_user: 供资源缓存同步用例使用
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name") values
    ('3248fb0d-0000-0000-0000-000000000054', '3248fb0d-0000-0000-0000-000000000050', '3248fb0d-0000-0000-0000-000000000055', 'system', '系统');
