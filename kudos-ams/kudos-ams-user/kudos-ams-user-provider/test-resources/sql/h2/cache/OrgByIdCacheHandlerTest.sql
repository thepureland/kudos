merge into "user_org" ("id", "name", "short_name", "tenant_id", "parent_id", "org_type_dict_code", "sort_num", "remark", "active", "built_in", "create_user_id", "create_user_name", "update_user_id", "update_user_name")
    values ('38a5e0b2-1111-1111-1111-111111111111', '技术部', '技术', 'tenant-001-hGyXOmCA', null, 'ORG_TYPE_TECH', 1, '技术研发机构', true, false, 'admin', '管理员', null, null),
           ('38a5e0b2-2222-2222-2222-222222222222', '产品部', '产品', 'tenant-001-hGyXOmCA', null, 'ORG_TYPE_PRODUCT', 2, '产品策划机构', true, false, 'admin', '管理员', null, null),
           ('38a5e0b2-3333-3333-3333-333333333333', '运营部', '运营', 'tenant-001-hGyXOmCA', null, 'ORG_TYPE_OPERATION', 3, '运营推广机构', true, false, 'admin', '管理员', null, null),
           ('38a5e0b2-4444-4444-4444-444444444444', '前端组', '前端', 'tenant-001-hGyXOmCA', '38a5e0b2-1111-1111-1111-111111111111', 'ORG_TYPE_TECH', 11, '前端开发组', true, false, 'admin', '管理员', null, null),
           ('38a5e0b2-5555-5555-5555-555555555555', '后端组', '后端', 'tenant-001-hGyXOmCA', '38a5e0b2-1111-1111-1111-111111111111', 'ORG_TYPE_TECH', 12, '后端开发组', true, false, 'admin', '管理员', null, null),
           ('38a5e0b2-6666-6666-6666-666666666666', '测试组', '测试', 'tenant-001-hGyXOmCA', '38a5e0b2-1111-1111-1111-111111111111', 'ORG_TYPE_TECH', 13, '测试团队', true, false, 'admin', '管理员', null, null),
           ('38a5e0b2-7777-7777-7777-777777777777', '财务部', '财务', 'tenant-001-hGyXOmCA', null, 'ORG_TYPE_FINANCE', 4, '财务管理机构', false, false, 'admin', '管理员', null, null),
           ('38a5e0b2-8888-8888-8888-888888888888', '人事部', '人事', 'tenant-001-hGyXOmCA', null, 'ORG_TYPE_HR', 5, '人力资源机构', true, true, 'system', '系统', null, null),
           ('38a5e0b2-9999-9999-9999-999999999999', '总部', '总部', 'tenant-002-hGyXOmCA', null, 'ORG_TYPE_HQ', 1, '总部', true, true, 'system', '系统', null, null);
