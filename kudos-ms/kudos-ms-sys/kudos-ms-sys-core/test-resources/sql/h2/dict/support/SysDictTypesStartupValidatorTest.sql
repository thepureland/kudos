-- SysDictTypesStartupValidatorTest 用例数据。
-- 用独立的 atomic_service_code (`sys-validator-test`) 隔离，避免与其它 dict 测试共享的 `'sys'` 行冲突。
-- 故意只覆盖 SysDictTypes 中的部分常量 + 一条「数据库有但常量未声明」的额外行 + 一条 active=false。

merge into "sys_dict" ("id", "dict_type", "dict_name", "atomic_service_code", "remark", "active", "built_in") values
    ('a0000000-0000-0000-0000-000000000001', 'ds_use',           'ds_use',           'sys-validator-test', 'startup-validator-test', true,  false),
    ('a0000000-0000-0000-0000-000000000002', 'ds_type',          'ds_type',          'sys-validator-test', 'startup-validator-test', true,  false),
    ('a0000000-0000-0000-0000-000000000003', 'resource_type',    'resource_type',    'sys-validator-test', 'startup-validator-test', true,  false),
    ('a0000000-0000-0000-0000-000000000004', 'cache_strategy',   'cache_strategy',   'sys-validator-test', 'startup-validator-test', true,  false),
    ('a0000000-0000-0000-0000-000000000005', 'locale',           'locale',           'sys-validator-test', 'startup-validator-test', true,  false),
    ('a0000000-0000-0000-0000-000000000006', 'i18n_type',        'i18n_type',        'sys-validator-test', 'startup-validator-test', true,  false),
    ('a0000000-0000-0000-0000-000000000007', 'timezone',         'timezone',         'sys-validator-test', 'startup-validator-test', true,  false),
    ('a0000000-0000-0000-0000-000000000008', 'domain_type',      'domain_type',      'sys-validator-test', 'startup-validator-test', true,  false),
    ('a0000000-0000-0000-0000-000000000009', 'terminal_type',    'terminal_type',    'sys-validator-test', 'startup-validator-test', true,  false),
    -- 未插入：ip_type、access_rule_type → 期望出现在 missing
    -- 额外（不在 SysDictTypes 中声明）：legacy_only
    ('a0000000-0000-0000-0000-00000000000a', 'legacy_only',      'legacy_only',      'sys-validator-test', 'startup-validator-test', true,  false),
    -- active=false 的不应被计入 extras（active 过滤）
    ('a0000000-0000-0000-0000-00000000000b', 'deactivated_type', 'deactivated_type', 'sys-validator-test', 'startup-validator-test', false, false);
