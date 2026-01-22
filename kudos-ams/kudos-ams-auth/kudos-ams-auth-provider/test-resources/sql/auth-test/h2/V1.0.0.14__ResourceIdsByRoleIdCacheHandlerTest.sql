-- 测试数据 for ResourceIdsByRoleIdCacheHandler
-- 需要完整的角色、角色-资源关系链

-- 角色数据
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('11111111-1111-1111-1111-111111111111', 'ROLE_ADMIN', '系统管理员', 'tenant-001', 'ams', '系统管理员角色', true, true, 'system', '系统', null, null),
           ('22222222-2222-2222-2222-222222222222', 'ROLE_USER', '普通用户', 'tenant-001', 'ams', '普通用户角色', true, false, 'admin', '管理员', null, null),
           ('33333333-3333-3333-3333-333333333333', 'ROLE_GUEST', '访客', 'tenant-001', 'ams', '访客角色', true, false, 'admin', '管理员', null, null);

-- 角色-资源关系
merge into "auth_role_resource" ("id", "role_id", "resource_id", "create_user_id", "create_user_name")
    values ('res1res1-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'resource-aaa', 'system', '系统'),
           ('res2res2-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'resource-bbb', 'system', '系统'),
           ('res3res3-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'resource-ccc', 'admin', '管理员'),
           ('res4res4-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222', 'resource-ddd', 'admin', '管理员');
