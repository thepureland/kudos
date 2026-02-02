-- 测试数据 for UserIdsByGroupIdCacheHandler
-- 需要完整的用户、用户组、用户组-用户关系链

-- 用户数据
merge into "user_account" ("id", "username", "tenant_id", "login_password", "display_name", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('5e90ce80-1111-1111-1111-111111111111', 'admin', 'tenant-001-twyuFAaV', 'password123-twyuFAaV', '管理员', '00000000-0000-0000-0000-000000000000', '系统管理员', true, true, 'system', '系统', null, null),
           ('5e90ce80-2222-2222-2222-222222222222', 'zhangsan', 'tenant-001-twyuFAaV', 'password123-twyuFAaV', '张三', '5e90ce80-1111-1111-1111-111111111111', '普通用户', true, false, 'admin', '管理员', null, null),
           ('5e90ce80-3333-3333-3333-333333333333', 'lisi', 'tenant-001-twyuFAaV', 'password123-twyuFAaV', '李四', '5e90ce80-1111-1111-1111-111111111111', '无用户组用户', true, false, 'admin', '管理员', null, null);

-- 用户组数据
merge into "auth_group" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('6e90ce80-1111-1111-1111-111111111111', 'GROUP_ADMIN', '管理员组', 'tenant-001-twyuFAaV', 'ams', '管理员用户组', true, true, 'system', '系统', null, null),
           ('6e90ce80-2222-2222-2222-222222222222', 'GROUP_USER', '普通用户组', 'tenant-001-twyuFAaV', 'ams', '普通用户组', true, false, 'admin', '管理员', null, null),
           ('6e90ce80-3333-3333-3333-333333333333', 'GROUP_GUEST', '访客组', 'tenant-001-twyuFAaV', 'ams', '访客用户组', true, false, 'admin', '管理员', null, null);

-- 用户组-用户关系
merge into "auth_group_user" ("id", "group_id", "user_id", "create_user_id", "create_user_name")
    values ('6e90ce80-1111-1111-1111-111111111111', '6e90ce80-1111-1111-1111-111111111111', '5e90ce80-1111-1111-1111-111111111111', 'system', '系统'),
           ('6e90ce80-2222-2222-2222-222222222222', '6e90ce80-2222-2222-2222-222222222222', '5e90ce80-2222-2222-2222-222222222222', 'admin', '管理员'),
           ('6e90ce80-3333-3333-3333-333333333333', '6e90ce80-1111-1111-1111-111111111111', '5e90ce80-2222-2222-2222-222222222222', 'admin', '管理员');
