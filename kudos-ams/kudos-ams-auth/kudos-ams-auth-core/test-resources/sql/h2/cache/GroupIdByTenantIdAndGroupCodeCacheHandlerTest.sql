-- 测试数据 for GroupIdByTenantIdAndGroupCodeCacheHandler
merge into "auth_group" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('7e2b8b93-1111-1111-1111-111111111111', 'GROUP_ADMIN', '管理员组', 'tenant-001-yoCqktm5', 'ams', '管理员用户组', true, true, 'system', '系统', null, null),
           ('7e2b8b93-2222-2222-2222-222222222222', 'GROUP_USER', '普通用户组', 'tenant-001-yoCqktm5', 'ams', '普通用户组', true, false, 'admin', '管理员', null, null),
           ('7e2b8b93-3333-3333-3333-333333333333', 'GROUP_GUEST', '访客组', 'tenant-001-yoCqktm5', 'ams', '访客用户组', true, false, 'admin', '管理员', null, null),
           ('7e2b8b93-4444-4444-4444-444444444444', 'GROUP_TEST', '测试用户组', 'tenant-001-yoCqktm5', 'ams', '测试用(inactive)', false, false, 'admin', '管理员', null, null),
           ('7e2b8b93-5555-5555-5555-555555555555', 'GROUP_ADMIN', '管理员组', 'tenant-002-yoCqktm5', 'ams', '租户2管理员组', true, true, 'system', '系统', null, null);
