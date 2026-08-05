-- 权限版本号用例：两个用户各持一个角色，另有一个"额外角色"用于制造权限变更。
-- 角色须绑定资源，否则解析出的授权项为空，指纹恒为常量，测不出"变更会移动版本号"。

merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('3f2c9b10-0000-0000-0000-0000000000a1', 'pv-user-1-3f2c9b10', 'pv-tenant-3f2c9b10', 'pwd', '00000000-0000-0000-0000-000000000000', 'from PermissionVersionServiceTest', true, false, 'system', '系统'),
    ('3f2c9b10-0000-0000-0000-0000000000a2', 'pv-user-2-3f2c9b10', 'pv-tenant-3f2c9b10', 'pwd', '00000000-0000-0000-0000-000000000000', 'from PermissionVersionServiceTest', true, false, 'system', '系统');

merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('3f2c9b10-0000-0000-0000-0000000000b1', 'pv-role-base-3f2c9b10', 'pv-role-base', 'pv-tenant-3f2c9b10', 'ams', 'pv', true, false, 'system', '系统'),
    ('3f2c9b10-0000-0000-0000-0000000000b2', 'pv-role-extra-3f2c9b10', 'pv-role-extra', 'pv-tenant-3f2c9b10', 'ams', 'pv', true, false, 'system', '系统');

-- 每个角色绑一条 code 型权限，使其对决策有实际贡献
merge into "auth_role_resource" ("id", "role_id", "permission_code", "effect", "create_user_id", "create_user_name") values
    ('3f2c9b10-0000-0000-0000-0000000000c1', '3f2c9b10-0000-0000-0000-0000000000b1', 'pv:base:read', 'ALLOW', 'system', '系统'),
    ('3f2c9b10-0000-0000-0000-0000000000c2', '3f2c9b10-0000-0000-0000-0000000000b2', 'pv:extra:write', 'ALLOW', 'system', '系统');

-- 两个用户都直接持有 base 角色；extra 角色留给用例去绑
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name") values
    ('3f2c9b10-0000-0000-0000-0000000000d1', '3f2c9b10-0000-0000-0000-0000000000b1', '3f2c9b10-0000-0000-0000-0000000000a1', 'system', '系统'),
    ('3f2c9b10-0000-0000-0000-0000000000d2', '3f2c9b10-0000-0000-0000-0000000000b1', '3f2c9b10-0000-0000-0000-0000000000a2', 'system', '系统');
