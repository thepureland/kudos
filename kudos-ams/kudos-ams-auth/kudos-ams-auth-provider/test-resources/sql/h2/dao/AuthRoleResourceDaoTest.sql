-- 测试数据：AuthRoleResourceDaoTest
-- 使用唯一前缀 auth-roleres-dao-test-* 和唯一UUID确保测试数据隔离

-- 创建测试用的角色
merge into "auth_role" ("id", "code", "name", "tenant_id", "subsys_code", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('50000000-0000-0000-0000-000000000060', 'auth-role-roleres-dao-test-1', 'auth-role-roleres-dao-test-1-name', 'auth-tenant-roleres-dao-test-1', 'ams', 'from AuthRoleResourceDaoTest', true, false, 'system', '系统'),
           ('50000000-0000-0000-0000-000000000061', 'auth-role-roleres-dao-test-2', 'auth-role-roleres-dao-test-2-name', 'auth-tenant-roleres-dao-test-1', 'ams', 'from AuthRoleResourceDaoTest', true, false, 'system', '系统');

-- 创建测试用的资源（需要先创建 portal 和 sub_system）
merge into "sys_portal" ("code", "name", "remark", "active", "built_in")
    values ('auth-portal-roleres-dao-test-1', 'auth-portal-roleres-dao-test-1-name', 'from AuthRoleResourceDaoTest', true, false);

merge into "sys_sub_system" ("code", "name", "portal_code", "remark", "active", "built_in")
    values ('auth-subsys-roleres-dao-test-1', 'auth-subsys-roleres-dao-test-1-name', 'auth-portal-roleres-dao-test-1', 'from AuthRoleResourceDaoTest', true, false);

merge into "sys_resource" ("id", "name", "url", "resource_type_dict_code", "parent_id", "order_num", "icon", "sub_system_code", "remark", "active", "built_in")
    values ('50000000-0000-0000-0000-000000000062', 'sys-resource-roleres-dao-test-1', '/sys-resource-roleres-dao-test-1', 'M', null, 1, null, 'auth-subsys-roleres-dao-test-1', 'from AuthRoleResourceDaoTest', true, false),
           ('50000000-0000-0000-0000-000000000063', 'sys-resource-roleres-dao-test-2', '/sys-resource-roleres-dao-test-2', 'B', '50000000-0000-0000-0000-000000000062', 2, null, 'auth-subsys-roleres-dao-test-1', 'from AuthRoleResourceDaoTest', true, false);

-- 创建已存在的角色-资源关系（用于测试exists和searchRoleIdsByResourceId）
merge into "auth_role_resource" ("id", "role_id", "resource_id", "create_user_id", "create_user_name")
    values ('50000000-0000-0000-0000-000000000064', '50000000-0000-0000-0000-000000000060', '50000000-0000-0000-0000-000000000062', 'system', '系统'),
           ('50000000-0000-0000-0000-000000000065', '50000000-0000-0000-0000-000000000060', '50000000-0000-0000-0000-000000000063', 'system', '系统'),
           ('50000000-0000-0000-0000-000000000066', '50000000-0000-0000-0000-000000000061', '50000000-0000-0000-0000-000000000062', 'system', '系统');