-- 测试数据：SysResourceCacheHandler（按用例独立分组，互不重叠，字段长度符合 sys_resource 表）
-- 表约束：id char(36), name varchar(64), url varchar(256), sub_system_code varchar(32)，唯一(name, sub_system_code)
--
-- 数据分组：
-- 1) getResourceById / getResourcesByIds     → srch1xxx, sub_system=srch-sys-7f3e2d1c
-- 2) listResourcesBySubSysAndUrl / getResourceId → srch2xxx, sub_system=srch-sys-a1b2c3d4
-- 3) listResourcesBySubSysAndType / getResourceIds → srch3xxx, sub_system=srch-sys-e5f6a7b8
-- 4) syncOnUpdate                           → srch4001, sub_system=srch-sys-sync-upd
-- 5) syncOnUpdateWithOldUrl                 → srch4002, sub_system=srch-sys-sync-upd
-- 6) syncOnUpdateActive                     → srch5001, sub_system=srch-sys-sync-active（仅此用例）
-- 7) syncOnDelete                           → srch4003, sub_system=srch-sys-sync-del
-- 8) syncOnBatchDelete                      → srch6001/srch6002, sub_system=srch-sys-sync-batch

merge into "sys_resource" ("id", "name", "url", "resource_type_dict_code", "parent_id", "order_num", "icon", "sub_system_code", "remark", "active") values
-- 1) 主键查询
('srch1001-1a2b-4c5d-8e9f-000000000001', 'srch-name-1a2b4c5d', '/srch/url/1a2b4c5d/001', '1', null, 1, null, 'srch-sys-7f3e2d1c', 'srch-remark-001', true),
('srch1002-2b3c-5d6e-9f0a-000000000002', 'srch-name-2b3c5d6e', '/srch/url/2b3c5d6e/002', '1', null, 2, null, 'srch-sys-7f3e2d1c', null, true),
('srch1003-3c4d-6e7f-0a1b-000000000003', 'srch-name-3c4d6e7f', '/srch/url/3c4d6e7f/003', '2', null, 3, null, 'srch-sys-7f3e2d1c', null, true),
-- 2) 子系统+URL
('srch2001-4d5e-7f8a-1b2c-000000000011', 'srch-name-a1b2c3d4-1', '/srch/suburl/a1b2/p01', '1', null, 1, null, 'srch-sys-a1b2c3d4', null, true),
('srch2002-5e6f-8a9b-2c3d-000000000012', 'srch-name-a1b2c3d4-2', '/srch/suburl/a1b2/p02', '1', null, 2, null, 'srch-sys-a1b2c3d4', null, true),
('srch2003-6f7a-9b0c-3d4e-000000000013', 'srch-name-a1b2c3d4-3', '/srch/suburl/a1b2/p03', '2', null, 3, null, 'srch-sys-a1b2c3d4', null, false),
('srch2004-7a8b-0c1d-4e5f-000000000014', 'srch-name-a1b2c3d4-4', null, '2', null, 4, null, 'srch-sys-a1b2c3d4', null, true),
-- 3) 子系统+资源类型
('srch3001-8b9c-1d2e-5f6a-000000000021', 'srch-name-e5f6a7b8-1', '/srch/type/e5f6/r01', '1', null, 1, null, 'srch-sys-e5f6a7b8', null, true),
('srch3002-9c0d-2e3f-6a7b-000000000022', 'srch-name-e5f6a7b8-2', '/srch/type/e5f6/r02', '1', null, 2, null, 'srch-sys-e5f6a7b8', null, true),
('srch3003-0d1e-3f4a-7b8c-000000000023', 'srch-name-e5f6a7b8-3', '/srch/type/e5f6/r03', '1', null, 3, null, 'srch-sys-e5f6a7b8', null, true),
('srch3004-1e2f-4a5b-8c9d-000000000024', 'srch-name-e5f6a7b8-4', '/srch/type/e5f6/r04', '2', null, 4, null, 'srch-sys-e5f6a7b8', null, true),
('srch3005-2f3a-5b6c-9d0e-000000000025', 'srch-name-e5f6a7b8-5', '/srch/type/e5f6/r05', '2', null, 5, null, 'srch-sys-e5f6a7b8', null, false),
-- 4) syncOnUpdate 专用
('srch4001-3a4b-6c7d-0e1f-000000000031', 'srch-name-sync-upd-1', '/srch/sync/upd/001', '1', null, 1, null, 'srch-sys-sync-upd', null, true),
-- 5) syncOnUpdateWithOldUrl 专用
('srch4002-4b5c-7d8e-1f2a-000000000032', 'srch-name-sync-upd-2', '/srch/sync/upd/002', '1', null, 2, null, 'srch-sys-sync-upd', null, true),
-- 6) syncOnUpdateActive 专用（仅此用例使用）
('srch5001-1a2b-4c5d-8e9f-000000000051', 'srch-name-sync-active-1', '/srch/sync/active/001', '1', null, 1, null, 'srch-sys-sync-active', null, true),
-- 7) syncOnDelete 专用
('srch4003-5c6d-8e9f-2a3b-000000000033', 'srch-name-sync-del-1', '/srch/sync/del/001', '2', null, 3, null, 'srch-sys-sync-del', null, true),
-- 8) syncOnBatchDelete 专用
('srch6001-2b3c-5d6e-9f0a-000000000061', 'srch-name-sync-batch-1', '/srch/sync/batch/001', '1', null, 1, null, 'srch-sys-sync-batch', null, true),
('srch6002-3c4d-6e7f-0a1b-000000000062', 'srch-name-sync-batch-2', '/srch/sync/batch/002', '1', null, 2, null, 'srch-sys-sync-batch', null, true);
