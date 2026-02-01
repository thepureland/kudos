-- 测试数据 for GroupIdsByUserIdCacheHandler
-- 需要完整的用户、用户组、用户-用户组关系链

-- 用户数据
merge into "user_account" ("id", "username", "tenant_id", "login_password", "display_name", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('88207878-1111-1111-1111-111111111111', 'admin', 'tenant-001-Gv4Pb40w', 'password123-Gv4Pb40w', '管理员', '00000000-0000-0000-0000-000000000000', '系统管理员', true, true, 'system', '系统', null, null),
           ('88207878-2222-2222-2222-222222222222', 'zhangsan', 'tenant-001-Gv4Pb40w', 'password123-Gv4Pb40w', '张三', '88207878-1111-1111-1111-111111111111', '普通用户', true, false, 'admin', '管理员', null, null),
           ('88207878-3333-3333-3333-333333333333', 'lisi', 'tenant-001-Gv4Pb40w', 'password123-Gv4Pb40w', '李四', '88207878-1111-1111-1111-111111111111', '无用户组用户', true, false, 'admin', '管理员', null, null);

-- 用户组数据
merge into "auth_group" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('88307878-1111-1111-1111-111111111111', 'GROUP_ADMIN', '管理员组', 'tenant-001-Gv4Pb40w', 'ams', '管理员用户组', true, true, 'system', '系统', null, null),
           ('88307878-2222-2222-2222-222222222222', 'GROUP_USER', '普通用户组', 'tenant-001-Gv4Pb40w', 'ams', '普通用户组', true, false, 'admin', '管理员', null, null);

-- 用户-用户组关系
merge into "auth_group_user" ("id", "group_id", "user_id", "create_user_id", "create_user_name")
    values ('88307878-1111-1111-1111-111111111111', '88307878-1111-1111-1111-111111111111', '88207878-1111-1111-1111-111111111111', 'system', '系统'),
           ('88307878-2222-2222-2222-222222222222', '88307878-2222-2222-2222-222222222222', '88207878-2222-2222-2222-222222222222', 'admin', '管理员'),
           ('88307878-3333-3333-3333-333333333333', '88307878-1111-1111-1111-111111111111', '88207878-2222-2222-2222-222222222222', 'admin', '管理员');
