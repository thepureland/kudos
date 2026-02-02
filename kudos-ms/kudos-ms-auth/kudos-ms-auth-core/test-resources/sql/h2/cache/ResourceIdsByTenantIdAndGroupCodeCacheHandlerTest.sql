-- 用户组数据（依赖数据）
merge into "auth_group" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('274d0234-1111-1111-1111-111111111111', 'GROUP_ADMIN', '管理员组', 'tenant-001-7h2QGcPi', 'ams', '管理员用户组', true, true, 'system', '系统', null, null),
           ('274d0234-2222-2222-2222-222222222222', 'GROUP_USER', '普通用户组', 'tenant-001-7h2QGcPi', 'ams', '普通用户组', true, false, 'admin', '管理员', null, null);

-- 角色数据（依赖数据）
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('274d0234-1111-1111-1111-111111111111', 'ROLE_ADMIN', '系统管理员', 'tenant-001-7h2QGcPi', 'ams', '系统管理员角色', true, true, 'system', '系统', null, null),
           ('274d0234-3333-3333-3333-333333333333', 'ROLE_GUEST', '访客', 'tenant-001-7h2QGcPi', 'ams', '访客角色', true, false, 'admin', '管理员', null, null);

-- 组-角色关系
merge into "auth_group_role" ("id", "group_id", "role_id", "create_user_id", "create_user_name")
    values ('274d0234-1111-1111-1111-111111111111', '274d0234-1111-1111-1111-111111111111', '274d0234-1111-1111-1111-111111111111', 'system', '系统'),
           ('274d0234-2222-2222-2222-222222222222', '274d0234-2222-2222-2222-222222222222', '274d0234-3333-3333-3333-333333333333', 'admin', '管理员');

-- 角色-资源关系
merge into "auth_role_resource" ("id", "role_id", "resource_id", "create_user_id", "create_user_name")
    values ('274d0234-1111-1111-1111-111111111111', '274d0234-1111-1111-1111-111111111111', 'resource-aaa', 'system', '系统'),
           ('274d0234-2222-2222-2222-222222222222', '274d0234-3333-3333-3333-333333333333', 'resource-bbb', 'admin', '管理员');
