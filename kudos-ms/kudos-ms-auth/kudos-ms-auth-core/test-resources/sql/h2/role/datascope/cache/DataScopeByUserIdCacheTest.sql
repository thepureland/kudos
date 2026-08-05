-- user_org: 机构树 root(d0) <- child(d1) <- grand(d2)，外加独立机构 other(d3)，用于数据权限解析用例
merge into "user_org" ("id", "name", "tenant_id", "parent_id", "org_type_dict_code", "active", "built_in", "create_user_id", "create_user_name") values
    ('6b8b6d1f-0000-0000-0000-0000000000d0', 'dsc-org-root', 'dsc-tenant-1-6b8b6d1f', null, 'ORG_TYPE_DS', true, false, 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-0000000000d1', 'dsc-org-child', 'dsc-tenant-1-6b8b6d1f', '6b8b6d1f-0000-0000-0000-0000000000d0', 'ORG_TYPE_DS', true, false, 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-0000000000d2', 'dsc-org-grand', 'dsc-tenant-1-6b8b6d1f', '6b8b6d1f-0000-0000-0000-0000000000d1', 'ORG_TYPE_DS', true, false, 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-0000000000d3', 'dsc-org-other', 'dsc-tenant-1-6b8b6d1f', null, 'ORG_TYPE_DS', true, false, 'system', '系统');

-- user_account: 各持不同数据权限角色，均属于 child(d1) 机构；noorg 无机构
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "org_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('6b8b6d1f-0000-0000-0000-0000000000e0', 'dsc-user-all-6b8b6d1f', 'dsc-tenant-1-6b8b6d1f', 'pwd', '00000000-0000-0000-0000-000000000000', '6b8b6d1f-0000-0000-0000-0000000000d1', 'ds', true, false, 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-0000000000e1', 'dsc-user-orgchild-6b8b6d1f', 'dsc-tenant-1-6b8b6d1f', 'pwd', '00000000-0000-0000-0000-000000000000', '6b8b6d1f-0000-0000-0000-0000000000d1', 'ds', true, false, 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-0000000000e2', 'dsc-user-org-6b8b6d1f', 'dsc-tenant-1-6b8b6d1f', 'pwd', '00000000-0000-0000-0000-000000000000', '6b8b6d1f-0000-0000-0000-0000000000d1', 'ds', true, false, 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-0000000000e3', 'dsc-user-self-6b8b6d1f', 'dsc-tenant-1-6b8b6d1f', 'pwd', '00000000-0000-0000-0000-000000000000', '6b8b6d1f-0000-0000-0000-0000000000d1', 'ds', true, false, 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-0000000000e4', 'dsc-user-custom-6b8b6d1f', 'dsc-tenant-1-6b8b6d1f', 'pwd', '00000000-0000-0000-0000-000000000000', '6b8b6d1f-0000-0000-0000-0000000000d1', 'ds', true, false, 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-0000000000e5', 'dsc-user-noorg-6b8b6d1f', 'dsc-tenant-1-6b8b6d1f', 'pwd', '00000000-0000-0000-0000-000000000000', null, 'ds', true, false, 'system', '系统');

-- auth_role: 每个角色一种 data_scope；f9 仅供 bindOrgs/getOrgIdsByRoleId 关系用例
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "data_scope", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('6b8b6d1f-0000-0000-0000-0000000000f0', 'dsc-role-all-6b8b6d1f', 'dsc-role-all', 'dsc-tenant-1-6b8b6d1f', 'ams', 'ALL', 'ds', true, false, 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-0000000000f1', 'dsc-role-orgchild-6b8b6d1f', 'dsc-role-orgchild', 'dsc-tenant-1-6b8b6d1f', 'ams', 'ORG_AND_CHILD', 'ds', true, false, 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-0000000000f2', 'dsc-role-org-6b8b6d1f', 'dsc-role-org', 'dsc-tenant-1-6b8b6d1f', 'ams', 'ORG', 'ds', true, false, 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-0000000000f3', 'dsc-role-self-6b8b6d1f', 'dsc-role-self', 'dsc-tenant-1-6b8b6d1f', 'ams', 'SELF', 'ds', true, false, 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-0000000000f4', 'dsc-role-custom-6b8b6d1f', 'dsc-role-custom', 'dsc-tenant-1-6b8b6d1f', 'ams', 'CUSTOM', 'ds', true, false, 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-0000000000f9', 'dsc-role-bindtest-6b8b6d1f', 'dsc-role-bindtest', 'dsc-tenant-1-6b8b6d1f', 'ams', 'CUSTOM', 'ds', true, false, 'system', '系统');

-- auth_role_user: 每个用户直绑其角色；noorg 用户持 ORG 角色（用于无机构回退用例）
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name") values
    ('6b8b6d1f-0000-0000-0000-00000000ae00', '6b8b6d1f-0000-0000-0000-0000000000f0', '6b8b6d1f-0000-0000-0000-0000000000e0', 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-00000000ae01', '6b8b6d1f-0000-0000-0000-0000000000f1', '6b8b6d1f-0000-0000-0000-0000000000e1', 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-00000000ae02', '6b8b6d1f-0000-0000-0000-0000000000f2', '6b8b6d1f-0000-0000-0000-0000000000e2', 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-00000000ae03', '6b8b6d1f-0000-0000-0000-0000000000f3', '6b8b6d1f-0000-0000-0000-0000000000e3', 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-00000000ae04', '6b8b6d1f-0000-0000-0000-0000000000f4', '6b8b6d1f-0000-0000-0000-0000000000e4', 'system', '系统'),
    ('6b8b6d1f-0000-0000-0000-00000000ae05', '6b8b6d1f-0000-0000-0000-0000000000f2', '6b8b6d1f-0000-0000-0000-0000000000e5', 'system', '系统');

-- auth_role_scope: custom 角色(f4) 在 org 维度上授权 other(d3)
merge into "auth_role_scope" ("id", "role_id", "dimension", "scope_value", "create_user_id", "create_user_name") values
    ('6b8b6d1f-0000-0000-0000-00000000a000', '6b8b6d1f-0000-0000-0000-0000000000f4', 'org', '6b8b6d1f-0000-0000-0000-0000000000d3', 'system', '系统');
