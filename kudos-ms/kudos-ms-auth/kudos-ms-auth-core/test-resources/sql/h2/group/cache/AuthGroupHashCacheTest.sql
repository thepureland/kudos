-- auth_group: 供 AuthGroupHashCache 测试使用，按 id 与 (tenant_id, code) 查询，不区分 active
merge into "auth_group" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name") values
    ('ag-hash-1111-1111-1111-1111111111111', 'GROUP_ADMIN', '管理员组', 'tenant-001-hashAuth', 'ams', '管理员用户组', true, true, 'system', '系统', null, null),
    ('ag-hash-2222-2222-2222-2222222222222', 'GROUP_USER', '普通用户组', 'tenant-001-hashAuth', 'ams', '普通用户组', true, false, 'admin', '管理员', null, null),
    ('ag-hash-3333-3333-3333-3333333333333', 'GROUP_GUEST', '访客组', 'tenant-001-hashAuth', 'ams', '访客用户组', true, false, 'admin', '管理员', null, null),
    ('ag-hash-4444-4444-4444-4444444444444', 'GROUP_TEST', '测试用户组', 'tenant-001-hashAuth', 'ams', '测试用', false, false, 'admin', '管理员', null, null),
    ('ag-hash-5555-5555-5555-5555555555555', 'GROUP_ADMIN', '管理员组', 'tenant-002-hashAuth', 'ams', '租户2管理员组', true, true, 'system', '系统', null, null);
