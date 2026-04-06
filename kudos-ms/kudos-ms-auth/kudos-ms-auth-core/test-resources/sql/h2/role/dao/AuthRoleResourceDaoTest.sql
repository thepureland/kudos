-- auth_role: 每条 id 唯一，使用 auth-roleres-dao-test-* 前缀隔离
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('49748162-0000-0000-0000-000000000060', 'auth-role-rol-dao-tes-1-mVUk8tb9', 'auth-rol-ro-da-te-1-nam-mVUk8tb9', 'auth-tena-rol-dao-tes-1-mVUk8tb9', 'ams', 'from AuthRoleResourceDaoTest', true, false, 'system', '系统'),
    ('49748162-0000-0000-0000-000000000061', 'auth-role-rol-dao-tes-2-mVUk8tb9', 'auth-rol-ro-da-te-2-nam-mVUk8tb9', 'auth-tena-rol-dao-tes-1-mVUk8tb9', 'ams', 'from AuthRoleResourceDaoTest', true, false, 'system', '系统');

-- sys_system: 供 sys_resource 的 sub_system_code 关联
merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in") values
    ('auth-port-rol-dao-tes-1-mVUk8tb9', 'auth-por-ro-da-te-1-nam-mVUk8tb9', null, false, 'from AuthRoleResourceDaoTest', true, false),
    ('auth-subs-rol-dao-tes-1-mVUk8tb9', 'auth-sub-ro-da-te-1-nam-mVUk8tb9', 'auth-port-rol-dao-tes-1-mVUk8tb9', true, 'from AuthRoleResourceDaoTest', true, false);

-- sys_resource: 每条 id 唯一，供角色-资源关联
merge into "sys_resource" ("id", "name", "url", "resource_type_dict_code", "parent_id", "order_num", "icon", "sub_system_code", "remark", "active", "built_in") values
    ('49748162-0000-0000-0000-000000000062', 'sys-reso-role-dao-tes-1-mVUk8tb9', '/sys-reso-rol-dao-tes-1-mVUk8tb9', 'M', null, 1, null, 'auth-subs-rol-dao-tes-1-mVUk8tb9', 'from AuthRoleResourceDaoTest', true, false),
    ('49748162-0000-0000-0000-000000000063', 'sys-reso-role-dao-tes-2-mVUk8tb9', '/sys-reso-rol-dao-tes-2-mVUk8tb9', 'B', '49748162-0000-0000-0000-000000000062', 2, null, 'auth-subs-rol-dao-tes-1-mVUk8tb9', 'from AuthRoleResourceDaoTest', true, false);

-- auth_role_resource: 已存在关系供 exists 和 searchRoleIdsByResourceId 用例使用
merge into "auth_role_resource" ("id", "role_id", "resource_id", "create_user_id", "create_user_name") values
    ('49748162-0000-0000-0000-000000000064', '49748162-0000-0000-0000-000000000060', '49748162-0000-0000-0000-000000000062', 'system', '系统'),
    ('49748162-0000-0000-0000-000000000065', '49748162-0000-0000-0000-000000000060', '49748162-0000-0000-0000-000000000063', 'system', '系统'),
    ('49748162-0000-0000-0000-000000000066', '49748162-0000-0000-0000-000000000061', '49748162-0000-0000-0000-000000000062', 'system', '系统');
