-- 测试数据 for UserIdByTenantIdAndUsernameCacheHandler
merge into "auth_user" ("id", "username", "tenant_id", "login_password", "display_name", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('11111111-1111-1111-1111-111111111111', 'admin', 'tenant-001', 'password123', '管理员', '00000000-0000-0000-0000-000000000000', '系统管理员', true, true, 'system', '系统', null, null),
           ('22222222-2222-2222-2222-222222222222', 'zhangsan', 'tenant-001', 'password123', '张三', '11111111-1111-1111-1111-111111111111', '普通用户', true, false, 'admin', '管理员', null, null),
           ('33333333-3333-3333-3333-333333333333', 'lisi', 'tenant-001', 'password123', '李四', '11111111-1111-1111-1111-111111111111', '普通用户', true, false, 'admin', '管理员', null, null),
           ('44444444-4444-4444-4444-444444444444', 'wangwu', 'tenant-001', 'password123', '王五', '11111111-1111-1111-1111-111111111111', '测试用户(inactive)', false, false, 'admin', '管理员', null, null),
           ('55555555-5555-5555-5555-555555555555', 'zhaoliu', 'tenant-002', 'password123', '赵六', '00000000-0000-0000-0000-000000000000', '租户2用户', true, false, 'admin', '管理员', null, null);
