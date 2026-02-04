-- auth_group: 每条 id 唯一，(tenant_id, code) 供 GroupById 缓存使用
merge into "auth_group" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name") values
    ('bd9f1e96-1111-1111-1111-aaaaaaaaaaaa', 'GROUP_ADMIN', '管理员组', 'tenant-001-XcBnCTdE', 'ams', '管理员用户组', true, true, 'system', '系统', null, null),
    ('bd9f1e96-2222-2222-2222-bbbbbbbbbbbb', 'GROUP_USER', '普通用户组', 'tenant-001-XcBnCTdE', 'ams', '普通用户组', true, false, 'admin', '管理员', null, null),
    ('bd9f1e96-3333-3333-3333-cccccccccccc', 'GROUP_GUEST', '访客组', 'tenant-001-XcBnCTdE', 'ams', '访客用户组', true, false, 'admin', '管理员', null, null),
    ('bd9f1e96-4444-4444-4444-dddddddddddd', 'GROUP_TEST', '测试用户组', 'tenant-001-XcBnCTdE', 'ams', '测试用', false, false, 'admin', '管理员', null, null),
    ('bd9f1e96-5555-5555-5555-eeeeeeeeeeee', 'GROUP_ADMIN', '管理员组', 'tenant-002-XcBnCTdE', 'ams', '租户2管理员组', true, true, 'system', '系统', null, null);
