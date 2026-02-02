-- 测试数据：SysDictItemDaoTest（每条 id 唯一）

merge into "sys_micro_service" ("code", "name", "remark", "active", "built_in") values
('svc-as-dictitem-dao-test-1_2921', 'svc-as-dictitem-dao-test-1_2921-name', 'from SysDictItemDaoTest', true, false);

merge into "sys_dict" ("id", "dict_type", "dict_name", "atomic_service_code", "remark", "active", "built_in") values
('40000000-0000-0000-0000-000000002921', 'svc-dict-ditem-dao-test-1', 'svc-dict-dictitem-dao-test-1-name', 'svc-module-ditem-dao-test-1', 'from SysDictItemDaoTest', true, false),
('40000000-0000-0000-0000-000000002922', 'svc-dict-ditem-dao-test-2', 'svc-dict-dictitem-dao-test-2-name', 'svc-module-ditem-dao-test-1', 'from SysDictItemDaoTest', false, false);

merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "parent_id", "remark", "active", "built_in") values
('40000000-0000-0000-0000-000000002921', '40000000-0000-0000-0000-000000002921', 'svc-item-ditem-dao-test-1', 'svc-item-dictitem-dao-test-1-name', 1, null, 'from SysDictItemDaoTest', true, false),
('40000000-0000-0000-0000-000000002922', '40000000-0000-0000-0000-000000002921', 'svc-item-ditem-dao-test-2', 'svc-item-dictitem-dao-test-2-name', 2, null, 'from SysDictItemDaoTest', true, false),
('40000000-0000-0000-0000-000000002923', '40000000-0000-0000-0000-000000002921', 'svc-item-ditem-dao-test-3', 'svc-item-dictitem-dao-test-3-name', 3, '40000000-0000-0000-0000-000000002921', 'from SysDictItemDaoTest', true, false),
('40000000-0000-0000-0000-000000002924', '40000000-0000-0000-0000-000000002921', 'svc-item-ditem-dao-test-4', 'svc-item-dictitem-dao-test-4-name', 4, null, 'from SysDictItemDaoTest', false, false);
