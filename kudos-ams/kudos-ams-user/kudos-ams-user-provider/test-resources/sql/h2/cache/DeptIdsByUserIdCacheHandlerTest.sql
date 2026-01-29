-- 测试数据 for DeptIdsByUserIdCacheHandler
-- 需要完整的用户、部门、用户-部门关系链

-- 用户数据
merge into "auth_user" ("id", "username", "tenant_id", "login_password", "display_name", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('81cea00f-1111-1111-1111-111111111111', 'admin', 'tenant-001-EwK7LrFx', 'password123-EwK7LrFx', '管理员', '00000000-0000-0000-0000-000000000000', '系统管理员', true, true, 'system', '系统', null, null),
           ('81cea00f-2222-2222-2222-222222222222', 'zhangsan', 'tenant-001-EwK7LrFx', 'password123-EwK7LrFx', '张三', '81cea00f-1111-1111-1111-111111111111', '普通用户', true, false, 'admin', '管理员', null, null),
           ('81cea00f-3333-3333-3333-333333333333', 'lisi', 'tenant-001-EwK7LrFx', 'password123-EwK7LrFx', '李四', '81cea00f-1111-1111-1111-111111111111', '无部门用户', true, false, 'admin', '管理员', null, null);

-- 部门数据
merge into "auth_dept" ("id", "name", "short_name", "tenant_id", "parent_id", "dept_type_dict_code", "sort_num", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('81cea00f-1111-1111-1111-111111111111', '技术部', '技术', 'tenant-001-EwK7LrFx', null, 'DEPT_TYPE_TECH', 1, '技术研发部门', true, false, 'admin', '管理员', null, null),
           ('81cea00f-2222-2222-2222-222222222222', '产品部', '产品', 'tenant-001-EwK7LrFx', null, 'DEPT_TYPE_PRODUCT', 2, '产品策划部门', true, false, 'admin', '管理员', null, null);

-- 用户-部门关系
merge into "auth_dept_user" ("id", "dept_id", "user_id", "dept_admin", "create_user_id", "create_user_name")
    values ('81cea00f-1111-1111-1111-111111111111', '81cea00f-1111-1111-1111-111111111111', '81cea00f-1111-1111-1111-111111111111', true, 'system', '系统'),
           ('81cea00f-2222-2222-2222-222222222222', '81cea00f-1111-1111-1111-111111111111', '81cea00f-2222-2222-2222-222222222222', false, 'admin', '管理员'),
           ('81cea00f-3333-3333-3333-333333333333', '81cea00f-2222-2222-2222-222222222222', '81cea00f-2222-2222-2222-222222222222', false, 'admin', '管理员');
