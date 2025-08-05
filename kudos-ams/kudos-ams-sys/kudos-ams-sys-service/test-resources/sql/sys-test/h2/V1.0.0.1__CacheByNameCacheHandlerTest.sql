-- update "sys_cache" set "strategy_dict_code" = 'LOCAL_REMOTE' where "name" = 'SYS_CACHE_BY_NAME';

merge into "sys_cache" ("id", "name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time",
                         "ttl", "remark", "active", "built_in")
values ('14a9adc4-6bb5-45bd-96bb-111111111111', 'TEST_CACHE_1', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '测试缓存1', true, true),
       ('e5340806-97b4-43a4-84c6-222222222222', 'TEST_CACHE_2', 'ams-sys', 'LOCAL_REMOTE', false, false, 999999999,
        '测试缓存2', true, true),
       ('e5340806-97b4-43a4-84c6-333333333333', 'TEST_CACHE_3', 'ams-sys', 'LOCAL_REMOTE', false, false, 999999999,
        '测试缓存3', true, true),
       ('2da8e352-6e6f-4cd4-93e0-444444444444', 'TEST_CACHE_4', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '测试缓存4', true, true),
       ('2da8e352-6e6f-4cd4-93e0-555555555555', 'TEST_CACHE_5', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '测试缓存5', true, true),
       ('2da8e352-6e6f-4cd4-93e0-666666666666', 'TEST_CACHE_6', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '测试缓存6', false, true);