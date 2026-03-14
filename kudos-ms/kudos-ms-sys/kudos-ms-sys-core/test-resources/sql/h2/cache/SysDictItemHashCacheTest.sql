-- 测试数据：SysDictItemHashCache（视图 v_sys_dict_item = sys_dict_item left join sys_dict）
-- 使用前需在 sys_cache 中配置 SYS_DICT_ITEM__HASH（hash=true），见 V1.0.0.11__init_sys_cache.sql
--
-- 数据分组：
-- 1) getDictItemById / getDictItemsByIds           → sdih-item-001/002/003，dict=sdih-dict-001 (sdih-ms-7f3e2d1c, sdih-type-001)
-- 2) getDictItemByAtomicServiceCodeAndDictTypeAndItemCode → sdih-item-a1 (sdih-ms-a1b2c3d4, sdih-type-a1, sdih-code-p01)
-- 3) getDictItemsByAtomicServiceCodeAndDictType    → sdih-item-a1/a2 (sdih-ms-a1b2c3d4, sdih-type-a1)
-- 4) syncOnUpdate                                  → sdih-item-upd
-- 5) syncOnDelete                                  → sdih-item-del
-- 6) syncOnBatchDelete                             → sdih-item-b1, sdih-item-b2

merge into "sys_dict" ("id", "atomic_service_code", "dict_type", "dict_name", "remark", "active", "built_in") values
('sdih-d001-7f3e2d1c-000000000001', 'sdih-ms-7f3e2d1c', 'sdih-type-001', 'sdih-dict-name-001', null, true, false),
('sdih-da1-a1b2c3d4-000000000011', 'sdih-ms-a1b2c3d4', 'sdih-type-a1', 'sdih-dict-name-a1', null, true, false),
('sdih-dup-6c7d-0e1f-000000000031', 'sdih-ms-sync-upd', 'sdih-type-sync-1', 'sdih-dict-name-sync-1', null, true, false),
('sdih-ddel-8e9f-2a3b-000000000033', 'sdih-ms-sync-del', 'sdih-type-sync-del', 'sdih-dict-name-sync-del', null, true, false),
('sdih-db1-2b3c-5d6e-9f0a-000061', 'sdih-ms-sync-batch', 'sdih-type-batch', 'sdih-dict-name-batch', null, true, false);

merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "active", "built_in") values
-- 1) 主键查询
('sdih-i001-1a2b-4c5d-8e9f-000001', 'sdih-d001-7f3e2d1c-000000000001', 'sdih-code-001', 'sdih-item-name-001', 1, null, true, false),
('sdih-i002-2b3c-5d6e-9f0a-000002', 'sdih-d001-7f3e2d1c-000000000001', 'sdih-code-002', 'sdih-item-name-002', 2, null, true, false),
('sdih-i003-3c4d-6e7f-0a1b-000003', 'sdih-d001-7f3e2d1c-000000000001', 'sdih-code-003', 'sdih-item-name-003', 3, null, true, false),
-- 2) 按 atomicServiceCode + dictType + itemCode 单条
('sdih-ia1-4d5e-7f8a-1b2c-000011', 'sdih-da1-a1b2c3d4-000000000011', 'sdih-code-p01', 'sdih-item-name-p01', 1, null, true, false),
('sdih-ia2-5e6f-8a9b-2c3d-000012', 'sdih-da1-a1b2c3d4-000000000011', 'sdih-code-p02', 'sdih-item-name-p02', 2, null, true, false),
-- 4) syncOnUpdate
('sdih-iupd-6c7d-0e1f-000000000031', 'sdih-dup-6c7d-0e1f-000000000031', 'sdih-code-upd', 'sdih-item-name-upd', 1, null, true, false),
-- 5) syncOnDelete
('sdih-idel-8e9f-2a3b-000000000033', 'sdih-ddel-8e9f-2a3b-000000000033', 'sdih-code-del', 'sdih-item-name-del', 1, null, true, false),
-- 6) syncOnBatchDelete
('sdih-ib1-2b3c-5d6e-9f0a-000061', 'sdih-db1-2b3c-5d6e-9f0a-000061', 'sdih-code-b1', 'sdih-item-name-b1', 1, null, true, false),
('sdih-ib2-3c4d-6e7f-0a1b-000062', 'sdih-db1-2b3c-5d6e-9f0a-000061', 'sdih-code-b2', 'sdih-item-name-b2', 2, null, true, false);
