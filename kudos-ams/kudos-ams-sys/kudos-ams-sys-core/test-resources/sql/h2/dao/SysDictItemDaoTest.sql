-- 测试数据：SysDictItemDaoTest
-- 使用唯一前缀 svc-dictitem-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('svc-as-dictitem-dao-test-1', 'svc-as-dictitem-dao-test-1-name', 'from SysDictItemDaoTest', true, false);


merge into "sys_dict" ("id", "dict_type", "dict_name", "atomic_service_code", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000050', 'svc-dict-ditem-dao-test-1', 'svc-dict-dictitem-dao-test-1-name', 'svc-module-ditem-dao-test-1', 'from SysDictItemDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000051', 'svc-dict-ditem-dao-test-2', 'svc-dict-dictitem-dao-test-2-name', 'svc-module-ditem-dao-test-1', 'from SysDictItemDaoTest', false, false);

merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "parent_id", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000052', '40000000-0000-0000-0000-000000000050', 'svc-item-ditem-dao-test-1', 'svc-item-dictitem-dao-test-1-name', 1, null, 'from SysDictItemDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000053', '40000000-0000-0000-0000-000000000050', 'svc-item-ditem-dao-test-2', 'svc-item-dictitem-dao-test-2-name', 2, null, 'from SysDictItemDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000054', '40000000-0000-0000-0000-000000000050', 'svc-item-ditem-dao-test-3', 'svc-item-dictitem-dao-test-3-name', 3, '40000000-0000-0000-0000-000000000052', 'from SysDictItemDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000055', '40000000-0000-0000-0000-000000000050', 'svc-item-ditem-dao-test-4', 'svc-item-dictitem-dao-test-4-name', 4, null, 'from SysDictItemDaoTest', false, false);
