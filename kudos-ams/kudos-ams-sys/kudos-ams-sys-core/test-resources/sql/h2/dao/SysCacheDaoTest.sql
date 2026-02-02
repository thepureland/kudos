-- 测试数据：SysCacheDaoTest
-- 使用唯一前缀 svc-cache-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_micro_service"
 ("code", "name", "remark", "active", "built_in")
    values ('svc-as-cache-dao-test-1', 'svc-as-cache-dao-test-1-name', 'from SysCacheDaoTest', true, false);

merge into "sys_cache" ("id", "name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time", "ttl", "remark", "active", "built_in")
    values ('40000000-0000-0000-0000-000000000180', 'svc-cache-dao-test-1', 'svc-as-cache-dao-test-1', 'CACHE_LOCAL', true, true, 3600, 'from SysCacheDaoTest', true, false),
           ('40000000-0000-0000-0000-000000000181', 'svc-cache-dao-test-2', 'svc-as-cache-dao-test-1', 'CACHE_REMOTE', false, false, 7200, 'from SysCacheDaoTest', true, false);
