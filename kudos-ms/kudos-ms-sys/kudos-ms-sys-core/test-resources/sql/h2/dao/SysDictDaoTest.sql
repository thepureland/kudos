-- 测试数据：SysDictDaoTest
-- 使用唯一前缀 svc-dict-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_micro_service" ("code", "name", "remark", "active", "built_in") values
    ('svc-as-dict-dao-test-1_2361', 'svc-as-dict-dao-test-1_2361-name', 'from SysDictDaoTest', true, false);

merge into "sys_dict" ("id", "dict_type", "dict_name", "atomic_service_code", "remark", "active", "built_in") values
    ('40000000-0000-0000-0000-000000002361', 'svc-dict-dao-test-1', 'svc-dict-dao-test-1-name', 'svc-module-dict-dao-test-1', 'from SysDictDaoTest', true, false),
    ('40000000-0000-0000-0000-000000002361', 'svc-dict-dao-test-2', 'svc-dict-dao-test-2-name', 'svc-module-dict-dao-test-1', 'from SysDictDaoTest', true, false);
