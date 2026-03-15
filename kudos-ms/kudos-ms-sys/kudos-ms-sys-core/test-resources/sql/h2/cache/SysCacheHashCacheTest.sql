-- 测试数据：SysCacheHashCacheTest
-- 需在 sys_cache 中配置 SYS_CACHE__HASH（hash=true），并准备按 id/name/atomicServiceCode 查询的测试数据

merge into "sys_micro_service" ("code", "name", "remark", "active", "built_in") values
    ('ams-sys-hash-test', 'ams-sys-hash-test-name', 'from SysCacheHashCacheTest', true, false);

-- SYS_CACHE__HASH 配置项（供 Hash 缓存启用）
merge into "sys_cache" ("id", "name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time", "ttl", "remark", "active", "built_in", "hash") values
    ('a1000000-0000-0000-0000-000000001001', 'SYS_CACHE__HASH', 'ams-sys-hash-test', 'CACHE_LOCAL', true, true, 3600, 'Hash cache config', true, true, true);

-- 测试用缓存配置（2 条，同 atomic_service_code）
merge into "sys_cache" ("id", "name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time", "ttl", "remark", "active", "built_in", "hash") values
    ('a1000000-0000-0000-0000-000000001002', 'SYS_CACHE_HASH_TEST_1', 'ams-sys-hash-test', 'CACHE_LOCAL', true, true, 3600, 'SysCacheHashCacheTest 1', true, false, false),
    ('a1000000-0000-0000-0000-000000001003', 'SYS_CACHE_HASH_TEST_2', 'ams-sys-hash-test', 'CACHE_REMOTE', false, false, 7200, 'SysCacheHashCacheTest 2', true, false, false);
