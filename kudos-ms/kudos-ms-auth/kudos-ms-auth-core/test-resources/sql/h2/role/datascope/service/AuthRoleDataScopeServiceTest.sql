-- user_org: 机构树 root(d0) <- child(d1) <- grand(d2)，外加独立机构 other(d3)，用于数据权限解析用例
merge into "user_org" ("id", "name", "tenant_id", "parent_id", "org_type_dict_code", "active", "built_in", "create_user_id", "create_user_name") values
    ('5a7a5c0e-0000-0000-0000-0000000000d0', 'ds-org-root', 'ds-tenant-1-5a7a5c0e', null, 'ORG_TYPE_DS', true, false, 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-0000000000d1', 'ds-org-child', 'ds-tenant-1-5a7a5c0e', '5a7a5c0e-0000-0000-0000-0000000000d0', 'ORG_TYPE_DS', true, false, 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-0000000000d2', 'ds-org-grand', 'ds-tenant-1-5a7a5c0e', '5a7a5c0e-0000-0000-0000-0000000000d1', 'ORG_TYPE_DS', true, false, 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-0000000000d3', 'ds-org-other', 'ds-tenant-1-5a7a5c0e', null, 'ORG_TYPE_DS', true, false, 'system', '系统');

-- user_account: 各持不同数据权限角色，均属于 child(d1) 机构；noorg 无机构
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "org_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('5a7a5c0e-0000-0000-0000-0000000000e0', 'ds-user-all-5a7a5c0e', 'ds-tenant-1-5a7a5c0e', 'pwd', '00000000-0000-0000-0000-000000000000', '5a7a5c0e-0000-0000-0000-0000000000d1', 'ds', true, false, 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-0000000000e1', 'ds-user-orgchild-5a7a5c0e', 'ds-tenant-1-5a7a5c0e', 'pwd', '00000000-0000-0000-0000-000000000000', '5a7a5c0e-0000-0000-0000-0000000000d1', 'ds', true, false, 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-0000000000e2', 'ds-user-org-5a7a5c0e', 'ds-tenant-1-5a7a5c0e', 'pwd', '00000000-0000-0000-0000-000000000000', '5a7a5c0e-0000-0000-0000-0000000000d1', 'ds', true, false, 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-0000000000e3', 'ds-user-self-5a7a5c0e', 'ds-tenant-1-5a7a5c0e', 'pwd', '00000000-0000-0000-0000-000000000000', '5a7a5c0e-0000-0000-0000-0000000000d1', 'ds', true, false, 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-0000000000e4', 'ds-user-custom-5a7a5c0e', 'ds-tenant-1-5a7a5c0e', 'pwd', '00000000-0000-0000-0000-000000000000', '5a7a5c0e-0000-0000-0000-0000000000d1', 'ds', true, false, 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-0000000000e5', 'ds-user-noorg-5a7a5c0e', 'ds-tenant-1-5a7a5c0e', 'pwd', '00000000-0000-0000-0000-000000000000', null, 'ds', true, false, 'system', '系统');

-- auth_role: 每个角色一种 data_scope；f9 仅供 bindOrgs/getOrgIdsByRoleId 关系用例
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "data_scope", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('5a7a5c0e-0000-0000-0000-0000000000f0', 'ds-role-all-5a7a5c0e', 'ds-role-all', 'ds-tenant-1-5a7a5c0e', 'ams', 'ALL', 'ds', true, false, 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-0000000000f1', 'ds-role-orgchild-5a7a5c0e', 'ds-role-orgchild', 'ds-tenant-1-5a7a5c0e', 'ams', 'ORG_AND_CHILD', 'ds', true, false, 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-0000000000f2', 'ds-role-org-5a7a5c0e', 'ds-role-org', 'ds-tenant-1-5a7a5c0e', 'ams', 'ORG', 'ds', true, false, 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-0000000000f3', 'ds-role-self-5a7a5c0e', 'ds-role-self', 'ds-tenant-1-5a7a5c0e', 'ams', 'SELF', 'ds', true, false, 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-0000000000f4', 'ds-role-custom-5a7a5c0e', 'ds-role-custom', 'ds-tenant-1-5a7a5c0e', 'ams', 'CUSTOM', 'ds', true, false, 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-0000000000f9', 'ds-role-bindtest-5a7a5c0e', 'ds-role-bindtest', 'ds-tenant-1-5a7a5c0e', 'ams', 'CUSTOM', 'ds', true, false, 'system', '系统');

-- auth_role_user: 每个用户直绑其角色；noorg 用户持 ORG 角色（用于无机构回退用例）
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name") values
    ('5a7a5c0e-0000-0000-0000-00000000ae00', '5a7a5c0e-0000-0000-0000-0000000000f0', '5a7a5c0e-0000-0000-0000-0000000000e0', 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-00000000ae01', '5a7a5c0e-0000-0000-0000-0000000000f1', '5a7a5c0e-0000-0000-0000-0000000000e1', 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-00000000ae02', '5a7a5c0e-0000-0000-0000-0000000000f2', '5a7a5c0e-0000-0000-0000-0000000000e2', 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-00000000ae03', '5a7a5c0e-0000-0000-0000-0000000000f3', '5a7a5c0e-0000-0000-0000-0000000000e3', 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-00000000ae04', '5a7a5c0e-0000-0000-0000-0000000000f4', '5a7a5c0e-0000-0000-0000-0000000000e4', 'system', '系统'),
    ('5a7a5c0e-0000-0000-0000-00000000ae05', '5a7a5c0e-0000-0000-0000-0000000000f2', '5a7a5c0e-0000-0000-0000-0000000000e5', 'system', '系统');

-- auth_role_org: custom 角色(f4) 授权 other(d3)
merge into "auth_role_org" ("id", "role_id", "org_id", "create_user_id", "create_user_name") values
    ('5a7a5c0e-0000-0000-0000-00000000a000', '5a7a5c0e-0000-0000-0000-0000000000f4', '5a7a5c0e-0000-0000-0000-0000000000d3', 'system', '系统');
