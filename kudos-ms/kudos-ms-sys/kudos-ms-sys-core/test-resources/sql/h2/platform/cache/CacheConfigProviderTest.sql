-- Test data: CacheConfigProviderTest
-- Unique prefix ccp-test-* keeps the rows isolated from other tests' data.

merge into "sys_micro_service" ("code", "name", "remark", "active", "built_in") values
    ('svc-as-ccp-test-1_9447', 'svc-as-ccp-test-1_9447-name', 'from CacheConfigProviderTest', true, false);

merge into "sys_cache" ("id", "name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time", "ttl", "remark", "active", "built_in", "hash") values
    ('90000000-0000-0000-0000-000000009447', 'ccp-test-local_9447',        'svc-as-ccp-test-1_9447', 'SINGLE_LOCAL', true,  true,  3600, 'from CacheConfigProviderTest', true,  false, false),
    ('90000000-0000-0000-0000-000000019447', 'ccp-test-remote_9447',       'svc-as-ccp-test-1_9447', 'REMOTE',       false, true,  3600, 'from CacheConfigProviderTest', true,  false, false),
    ('90000000-0000-0000-0000-000000029447', 'ccp-test-local-remote_9447', 'svc-as-ccp-test-1_9447', 'LOCAL_REMOTE', false, false, 7200, 'from CacheConfigProviderTest', true,  false, false),
    ('90000000-0000-0000-0000-000000039447', 'ccp-test-hash_9447',         'svc-as-ccp-test-1_9447', 'REMOTE',       false, true,  3600, 'from CacheConfigProviderTest', true,  false, true),
    ('90000000-0000-0000-0000-000000049447', 'ccp-test-inactive_9447',     'svc-as-ccp-test-1_9447', 'REMOTE',       false, true,  3600, 'from CacheConfigProviderTest', false, false, false);
