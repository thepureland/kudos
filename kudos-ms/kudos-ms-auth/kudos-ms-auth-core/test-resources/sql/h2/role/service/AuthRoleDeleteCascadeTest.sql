-- user_account: uX 直接持有 roleA/roleB；uY 通过用户组 g1 继承 roleA
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('3fae12cd-0000-0000-0000-0000000000b1', 'cascade-user-1-3fae12cd', 'cascade-tenant-3fae12cd', 'pwd', '00000000-0000-0000-0000-000000000000', 'from AuthRoleDeleteCascadeTest', true, false, 'system', '系统'),
    ('3fae12cd-0000-0000-0000-0000000000b2', 'cascade-user-2-3fae12cd', 'cascade-tenant-3fae12cd', 'pwd', '00000000-0000-0000-0000-000000000000', 'from AuthRoleDeleteCascadeTest', true, false, 'system', '系统');

-- auth_role: roleA 为级联删除主角；roleB 供 batchDelete 用例；roleC 为互斥对的另一方（应存活）
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('3fae12cd-0000-0000-0000-0000000000a1', 'cascade-role-a-3fae12cd', 'cascade-role-a', 'cascade-tenant-3fae12cd', 'ams', 'from AuthRoleDeleteCascadeTest', true, false, 'system', '系统'),
    ('3fae12cd-0000-0000-0000-0000000000a2', 'cascade-role-b-3fae12cd', 'cascade-role-b', 'cascade-tenant-3fae12cd', 'ams', 'from AuthRoleDeleteCascadeTest', true, false, 'system', '系统'),
    ('3fae12cd-0000-0000-0000-0000000000a3', 'cascade-role-c-3fae12cd', 'cascade-role-c', 'cascade-tenant-3fae12cd', 'ams', 'from AuthRoleDeleteCascadeTest', true, false, 'system', '系统');

-- auth_role_user: uX 直接持有 roleA 与 roleB
merge into "auth_role_user" ("id", "role_id", "user_id", "create_user_id", "create_user_name") values
    ('3fae12cd-0000-0000-0000-0000000000c1', '3fae12cd-0000-0000-0000-0000000000a1', '3fae12cd-0000-0000-0000-0000000000b1', 'system', '系统'),
    ('3fae12cd-0000-0000-0000-0000000000c2', '3fae12cd-0000-0000-0000-0000000000a2', '3fae12cd-0000-0000-0000-0000000000b1', 'system', '系统');

-- auth_role_resource: roleA -> res1, roleB -> res2（资源 id 不要求在 sys_resource 中存在，关系级断言足够）
merge into "auth_role_resource" ("id", "role_id", "resource_id", "create_user_id", "create_user_name") values
    ('3fae12cd-0000-0000-0000-0000000000c3', '3fae12cd-0000-0000-0000-0000000000a1', '3fae12cd-0000-0000-0000-0000000000e1', 'system', '系统'),
    ('3fae12cd-0000-0000-0000-0000000000c4', '3fae12cd-0000-0000-0000-0000000000a2', '3fae12cd-0000-0000-0000-0000000000e2', 'system', '系统');

-- auth_group: g1 绑定 roleA，uY 在 g1 中（组继承路径）
merge into "auth_group" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('3fae12cd-0000-0000-0000-0000000000d1', 'cascade-group-1-3fae12cd', 'cascade-group-1', 'cascade-tenant-3fae12cd', 'ams', 'from AuthRoleDeleteCascadeTest', true, false, 'system', '系统');

merge into "auth_group_user" ("id", "group_id", "user_id", "create_user_id", "create_user_name") values
    ('3fae12cd-0000-0000-0000-0000000000d2', '3fae12cd-0000-0000-0000-0000000000d1', '3fae12cd-0000-0000-0000-0000000000b2', 'system', '系统');

merge into "auth_group_role" ("id", "group_id", "role_id", "create_user_id", "create_user_name") values
    ('3fae12cd-0000-0000-0000-0000000000d3', '3fae12cd-0000-0000-0000-0000000000d1', '3fae12cd-0000-0000-0000-0000000000a1', 'system', '系统');

-- auth_role_org: roleA 的自定义数据权限机构授权
merge into "auth_role_org" ("id", "role_id", "org_id", "create_user_id", "create_user_name") values
    ('3fae12cd-0000-0000-0000-0000000000f1', '3fae12cd-0000-0000-0000-0000000000a1', '3fae12cd-0000-0000-0000-0000000000f9', 'system', '系统');

-- auth_role_exclusion: (roleA, roleC) 互斥对（canonical: role_a_id < role_b_id）
merge into "auth_role_exclusion" ("id", "role_a_id", "role_b_id", "tenant_id", "description", "create_user_id", "create_user_name") values
    ('3fae12cd-0000-0000-0000-0000000000f2', '3fae12cd-0000-0000-0000-0000000000a1', '3fae12cd-0000-0000-0000-0000000000a3', 'cascade-tenant-3fae12cd', 'cascade SoD pair', 'system', '系统');
