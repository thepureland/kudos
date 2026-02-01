-- 测试数据：SysSubSystemDaoTest
-- 使用唯一前缀 svc-subsys-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_system" ("code", "name", "remark", "active", "built_in")
    values ('svc-system-subsys-dao-test-1', 'svc-system-subsys-dao-test-1-name', 'from SysSubSystemDaoTest', true, false);

merge into "sys_sub_system" ("code", "name", "system_code", "remark", "active", "built_in")
    values ('svc-subsys-dao-test-1', 'svc-subsys-dao-test-1-name', 'svc-system-subsys-dao-test-1', 'from SysSubSystemDaoTest', true, false),
           ('svc-subsys-dao-test-2', 'svc-subsys-dao-test-2-name', 'svc-system-subsys-dao-test-1', 'from SysSubSystemDaoTest', true, false);
