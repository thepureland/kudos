merge into "sys_micro_service" ("code", "name", "remark", "active", "built_in") values
    ('svc-as-cache-test-1_7838', 'svc-as-cache-test-1_7838-name', 'from SysCacheServiceTest', true, false);

merge into "sys_cache" ("id", "name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time", "ttl", "remark", "active", "built_in") values
    ('20000000-0000-0000-0000-000000007838', 'svc-cache-test-1', 'svc-as-cache-test-1_7838', 'SINGLE_LOCAL', true, true, 3600, 'from SysCacheServiceTest', true, false);
