-- 测试数据：SysResourceDaoTest
-- 使用唯一前缀 svc-resource-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_portal" ("code", "name", "remark", "active", "built_in")
    values ('svc-portal-resource-dao-test-1', 'svc-portal-resource-dao-test-1-name', 'from SysResourceDaoTest', true, false);

merge into "sys_sub_system" ("code", "name", "portal_code", "remark", "active", "built_in")
    values ('svc-subsys-resource-dao-test-1', 'svc-subsys-resource-dao-test-1-name', 'svc-portal-resource-dao-test-1', 'from SysResourceDaoTest', true, false);

merge into "sys_resource" ("id", "name", "url", "resource_type_dict_code", "parent_id", "order_num", "icon", "sub_system_code", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000140', 'svc-resource-dao-test-1', '/svc-resource-dao-test-1', 'M', null, 1, null, 'svc-subsys-resource-dao-test-1', 'from SysResourceDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000141', 'svc-resource-dao-test-2', '/svc-resource-dao-test-2', 'B', '40000000-0000-0000-0000-000000000140', 2, null, 'svc-subsys-resource-dao-test-1', 'from SysResourceDaoTest', true, false);
