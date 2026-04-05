-- 测试数据：SysResourceDaoTest
-- 使用唯一前缀 svc-resource-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in") values
    ('svc-system-resource-dao-tes_1680', 'svc-system-resource-dao-test-1-name', null, false, 'from SysResourceDaoTest', true, false),
    ('svc-subsys-resource-dao-tes_1680', 'svc-subsys-resource-dao-tes_1680-name', 'svc-system-resource-dao-tes_1680', true, 'from SysResourceDaoTest', true, false);

merge into "sys_resource" ("id", "name", "url", "resource_type_dict_code", "parent_id", "order_num", "icon", "sub_system_code", "remark", "active", "built_in") values
    ('40000000-0000-0000-0000-000000001680', 'svc-resource-dao-test-1', '/svc-resource-dao-test-1', 'M', null, 1, null, 'svc-subsys-resource-dao-tes_1680', 'from SysResourceDaoTest', true, false),
    ('40000000-0000-0000-0000-000000001680', 'svc-resource-dao-test-2', '/svc-resource-dao-test-2', 'B', '40000000-0000-0000-0000-000000001680', 2, null, 'svc-subsys-resource-dao-tes_1680', 'from SysResourceDaoTest', true, false);
