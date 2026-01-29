-- 测试数据 for ResourceIdsByTenanetIdAndUsernameCacheHandler
-- 需要完整的用户、角色、用户-角色、角色-资源关系链

-- 用户数据
merge into "auth_user" ("id", "username", "tenant_id", "login_password", "display_name", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('8e232124-1111-1111-1111-111111111111', 'admin', 'tenant-001-InqhPsBT', 'password123-InqhPsBT', '管理员', '00000000-0000-0000-0000-000000000000', '系统管理员', true, true, 'system', '系统', null, null),
           ('8e232124-2222-2222-2222-222222222222', 'zhangsan', 'tenant-001-InqhPsBT', 'password123-InqhPsBT', '张三', '8e232124-1111-1111-1111-111111111111', '普通用户', true, false, 'admin', '管理员', null, null);

-- 角色数据
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('8e232124-1111-1111-1111-111111111111', 'ROLE_ADMIN', '系统管理员', 'tenant-001-InqhPsBT', 'ams', '系统管理员角色', true, true, 'system', '系统', null, null),
           ('8e232124-2222-2222-2222-222222222222', 'ROLE_USER', '普通用户', 'tenant-001-InqhPsBT', 'ams', '普通用户角色', true, false, 'admin', '管理员', null, null);

-- 用户-角色关系
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name")
    values ('8e232124-1111-1111-1111-111111111111', '8e232124-1111-1111-1111-111111111111', '8e232124-1111-1111-1111-111111111111', 'system', '系统'),
           ('8e232124-2222-2222-2222-222222222222', '8e232124-2222-2222-2222-222222222222', '8e232124-2222-2222-2222-222222222222', 'admin', '管理员');

-- 角色-资源关系
merge into "auth_role_resource" ("id", "role_id", "resource_id", "create_user_id", "create_user_name")
    values ('8e232124-1111-1111-1111-111111111111', '8e232124-1111-1111-1111-111111111111', 'resource-aaa-InqhPsBT', 'system', '系统'),
           ('8e232124-2222-2222-2222-222222222222', '8e232124-1111-1111-1111-111111111111', 'resource-bbb-InqhPsBT', 'system', '系统'),
           ('8e232124-3333-3333-3333-333333333333', '8e232124-2222-2222-2222-222222222222', 'resource-ccc-InqhPsBT', 'admin', '管理员');
