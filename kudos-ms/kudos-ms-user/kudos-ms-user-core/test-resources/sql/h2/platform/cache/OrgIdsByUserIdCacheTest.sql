-- user_org: 每条 id 唯一，81cea00f-* 供 OrgIdsByUserId 缓存用例使用（技术部/产品部）
merge into "user_org" ("id", "name", "short_name", "tenant_id", "parent_id", "org_type_dict_code", "sort_num", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('81cea00f-1111-1111-1111-111111111111', '技术部-OrgIdsByUserId', '技术', 'tenant-001-81cea00f', null, 'ORG_TYPE_TECH', 1, 'OrgIdsByUserIdCacheTest', true, false, 'system', '系统'),
    ('81cea00f-2222-2222-2222-222222222222', '产品部-OrgIdsByUserId', '产品', 'tenant-001-81cea00f', null, 'ORG_TYPE_PRODUCT', 2, 'OrgIdsByUserIdCacheTest', true, false, 'system', '系统');

-- user_org_user: 用户1111属于技术部；用户2222属于技术部+产品部；用户3333无机构（供 getOrgIds/syncOn* 用例）
merge into "user_org_user" ("id", "org_id", "user_id", "org_admin", "create_user_id", "create_user_name") values
    ('81cea00f-1111-1111-1111-111111111111', '81cea00f-1111-1111-1111-111111111111', '81cea00f-1111-1111-1111-111111111111', true, 'system', '系统'),
    ('81cea00f-2222-2222-2222-222222222221', '81cea00f-1111-1111-1111-111111111111', '81cea00f-2222-2222-2222-222222222222', false, 'system', '系统'),
    ('81cea00f-2222-2222-2222-222222222222', '81cea00f-2222-2222-2222-222222222222', '81cea00f-2222-2222-2222-222222222222', false, 'system', '系统');
