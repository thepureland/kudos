merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('11111111-1111-1111-1111-111111111111', 'ROLE_ADMIN', '系统管理员', 'tenant-001', 'ams', '系统管理员角色', true, true, 'system', '系统', null, null),
           ('22222222-2222-2222-2222-222222222222', 'ROLE_USER', '普通用户', 'tenant-001', 'ams', '普通用户角色', true, false, 'admin', '管理员', null, null),
           ('33333333-3333-3333-3333-333333333333', 'ROLE_GUEST', '访客', 'tenant-001', 'ams', '访客角色', true, false, 'admin', '管理员', null, null),
           ('44444444-4444-4444-4444-444444444444', 'ROLE_TEST', '测试角色', 'tenant-001', 'ams', '测试用', false, false, 'admin', '管理员', null, null),
           ('55555555-5555-5555-5555-555555555555', 'ROLE_ADMIN', '系统管理员', 'tenant-002', 'ams', '租户2管理员', true, true, 'system', '系统', null, null);
