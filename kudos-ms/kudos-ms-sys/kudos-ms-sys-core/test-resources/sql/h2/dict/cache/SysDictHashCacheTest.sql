-- 测试数据：SysDictHashCache（按用例独立分组，互不重叠）
-- 表约束：id 主键，唯一 (dict_type, atomic_service_code)
-- 使用前需在 sys_cache 中配置 SYS_DICT__HASH（hash=true），见 V1.0.0.11__init_sys_cache.sql
--
-- 数据分组：
-- 1) getDictById / getDictsByIds           → sdch1xxx, atomic_service_code=sdch-ms-7f3e2d1c
-- 2) getDictByAtomicServiceCodeAndDictType → sdch2xxx, atomic_service_code=sdch-ms-a1b2c3d4
-- 3) syncOnUpdate                          → sdch4001, atomic_service_code=sdch-ms-sync-upd
-- 4) syncOnDelete                         → sdch4003, atomic_service_code=sdch-ms-sync-del
-- 5) syncOnBatchDelete                    → sdch6001/sdch6002, atomic_service_code=sdch-ms-sync-batch

merge into "sys_dict" ("id", "atomic_service_code", "dict_type", "dict_name", "remark", "active", "built_in") values
-- 1) 主键查询
('sdch1001-1a2b-4c5d-8e9f-000000000001', 'sdch-ms-7f3e2d1c', 'sdch-type-001', 'sdch-dict-name-001', 'sdch-remark-001', true, false),
('sdch1002-2b3c-5d6e-9f0a-000000000002', 'sdch-ms-7f3e2d1c', 'sdch-type-002', 'sdch-dict-name-002', null, true, false),
('sdch1003-3c4d-6e7f-0a1b-000000000003', 'sdch-ms-7f3e2d1c', 'sdch-type-003', 'sdch-dict-name-003', null, true, false),
-- 2) 原子服务编码+字典类型（getDictByAtomicServiceCodeAndDictType 仅查 active=true）
('sdch2001-4d5e-7f8a-1b2c-000000000011', 'sdch-ms-a1b2c3d4', 'sdch-type-a1', 'sdch-dict-name-a1', null, true, false),
('sdch2002-5e6f-8a9b-2c3d-000000000012', 'sdch-ms-a1b2c3d4', 'sdch-type-a2', 'sdch-dict-name-a2', null, false, false),
-- 3) syncOnUpdate 专用
('sdch4001-3a4b-6c7d-0e1f-000000000031', 'sdch-ms-sync-upd', 'sdch-type-sync-1', 'sdch-dict-name-sync-1', null, true, false),
-- 4) syncOnDelete 专用
('sdch4003-5c6d-8e9f-2a3b-000000000033', 'sdch-ms-sync-del', 'sdch-type-sync-del', 'sdch-dict-name-sync-del', null, true, false),
-- 5) syncOnBatchDelete 专用
('sdch6001-2b3c-5d6e-9f0a-000000000061', 'sdch-ms-sync-batch', 'sdch-type-batch-1', 'sdch-dict-name-batch-1', null, true, false),
('sdch6002-3c4d-6e7f-0a1b-000000000062', 'sdch-ms-sync-batch', 'sdch-type-batch-2', 'sdch-dict-name-batch-2', null, true, false);
