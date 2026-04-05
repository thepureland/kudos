-- auth_role: 每条 id 唯一，供角色-资源关联
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name") values
    ('699180cb-1111-1111-1111-111111111111', 'ROLE_ADMIN', '系统管理员', 'tenant-001-wXxAqLrp', 'ams', '系统管理员角色', true, true, 'system', '系统', null, null),
    ('699180cb-2222-2222-2222-222222222222', 'ROLE_USER', '普通用户', 'tenant-001-wXxAqLrp', 'ams', '普通用户角色', true, false, 'admin', '管理员', null, null),
    ('699180cb-3333-3333-3333-333333333333', 'ROLE_GUEST', '访客', 'tenant-001-wXxAqLrp', 'ams', '访客角色', true, false, 'admin', '管理员', null, null);

-- auth_role_resource: 角色-资源关系
merge into "auth_role_resource" ("id", "role_id", "resource_id", "create_user_id", "create_user_name") values
    ('699180cb-1111-1111-1111-111111111111', '699180cb-1111-1111-1111-111111111111', 'resource-aaa-wXxAqLrp', 'system', '系统'),
    ('699180cb-2222-2222-2222-222222222222', '699180cb-1111-1111-1111-111111111111', 'resource-bbb-wXxAqLrp', 'system', '系统'),
    ('699180cb-3333-3333-3333-333333333333', '699180cb-2222-2222-2222-222222222222', 'resource-ccc-wXxAqLrp', 'admin', '管理员'),
    ('699180cb-4444-4444-4444-444444444444', '699180cb-2222-2222-2222-222222222222', 'resource-ddd-wXxAqLrp', 'admin', '管理员');
