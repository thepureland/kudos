-- 角色数据（依赖数据）
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('10796e8c-1111-1111-1111-111111111111', 'ROLE_ADMIN', '系统管理员', 'tenant-001-58TWQx6c', 'ams', '系统管理员角色', true, true, 'system', '系统', null, null),
           ('10796e8c-2222-2222-2222-222222222222', 'ROLE_USER', '普通用户', 'tenant-001-58TWQx6c', 'ams', '普通用户角色', true, false, 'admin', '管理员', null, null);

-- 角色-用户关系
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name")
    values ('10796e8c-1111-1111-1111-111111111111', '10796e8c-1111-1111-1111-111111111111', '10796e8c-1111-1111-1111-111111111111', 'system', '系统'),
           ('10796e8c-2222-2222-2222-222222222222', '10796e8c-2222-2222-2222-222222222222', '10796e8c-2222-2222-2222-222222222222', 'admin', '管理员');
