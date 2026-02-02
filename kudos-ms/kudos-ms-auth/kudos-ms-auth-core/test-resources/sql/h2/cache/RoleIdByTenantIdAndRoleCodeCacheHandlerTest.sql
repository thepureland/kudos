-- auth_role: 每条 id 唯一，(tenant_id, code) 供缓存 key 使用
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name") values
    ('6e2b8b93-1111-1111-1111-111111111111', 'ROLE_ADMIN', '系统管理员', 'tenant-001-yoCqktm5', 'ams', '系统管理员角色', true, true, 'system', '系统', null, null),
    ('6e2b8b93-2222-2222-2222-222222222222', 'ROLE_USER', '普通用户', 'tenant-001-yoCqktm5', 'ams', '普通用户角色', true, false, 'admin', '管理员', null, null),
    ('6e2b8b93-3333-3333-3333-333333333333', 'ROLE_GUEST', '访客', 'tenant-001-yoCqktm5', 'ams', '访客角色', true, false, 'admin', '管理员', null, null),
    ('6e2b8b93-4444-4444-4444-444444444444', 'ROLE_TEST', '测试角色', 'tenant-001-yoCqktm5', 'ams', '测试用(inactive)', false, false, 'admin', '管理员', null, null),
    ('6e2b8b93-5555-5555-5555-555555555555', 'ROLE_ADMIN', '系统管理员', 'tenant-002-yoCqktm5', 'ams', '租户2管理员', true, true, 'system', '系统', null, null);
