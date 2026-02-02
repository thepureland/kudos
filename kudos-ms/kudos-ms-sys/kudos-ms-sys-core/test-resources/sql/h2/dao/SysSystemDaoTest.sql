-- 测试数据：SysSystemDaoTest
-- 使用唯一前缀 svc-system-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_system" ("code", "name", "remark", "active", "built_in") values
    ('svc-system-dao-test-1_9315', 'svc-system-dao-test-1_9315-name', 'from SysSystemDaoTest', true, false),
    ('svc-system-dao-test-2_9315', 'svc-system-dao-test-2_9315-name', 'from SysSystemDaoTest', true, false);
