-- 角色数据（依赖数据）
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('174d0234-1111-1111-1111-111111111111', 'ROLE_ADMIN', '系统管理员', 'tenant-001-7h2QGcPi', 'ams', '系统管理员角色', true, true, 'system', '系统', null, null),
           ('174d0234-2222-2222-2222-222222222222', 'ROLE_USER', '普通用户', 'tenant-001-7h2QGcPi', 'ams', '普通用户角色', true, false, 'admin', '管理员', null, null);

-- 角色-资源关系
merge into "auth_role_resource" ("id", "role_id", "resource_id", "create_user_id", "create_user_name")
    values ('174d0234-1111-1111-1111-111111111111', '174d0234-1111-1111-1111-111111111111', 'resource-aaa-7h2QGcPi', 'system', '系统'),
           ('174d0234-2222-2222-2222-222222222222', '174d0234-1111-1111-1111-111111111111', 'resource-bbb-7h2QGcPi', 'system', '系统'),
           ('174d0234-3333-3333-3333-333333333333', '174d0234-2222-2222-2222-222222222222', 'resource-ccc-7h2QGcPi', 'admin', '管理员');
