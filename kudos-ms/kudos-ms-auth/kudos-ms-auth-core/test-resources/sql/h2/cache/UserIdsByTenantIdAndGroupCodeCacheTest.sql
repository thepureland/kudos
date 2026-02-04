-- auth_group: 每条 id 唯一，供组-用户关联
merge into "auth_group" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name") values
    ('20796e8c-1111-1111-1111-111111111111', 'GROUP_ADMIN', '管理员组', 'tenant-001-58TWQx6c', 'ams', '管理员用户组', true, true, 'system', '系统', null, null),
    ('20796e8c-2222-2222-2222-222222222222', 'GROUP_USER', '普通用户组', 'tenant-001-58TWQx6c', 'ams', '普通用户组', true, false, 'admin', '管理员', null, null);

-- auth_group_user: 组-用户关系
merge into "auth_group_user" ("id", "group_id", "user_id", "create_user_id", "create_user_name") values
    ('20796e8c-1111-1111-1111-111111111111', '20796e8c-1111-1111-1111-111111111111', '20796e8c-1111-1111-1111-111111111111', 'system', '系统'),
    ('20796e8c-2222-2222-2222-222222222222', '20796e8c-2222-2222-2222-222222222222', '20796e8c-2222-2222-2222-222222222222', 'admin', '管理员');
