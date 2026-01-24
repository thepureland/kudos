-- 测试数据：SysSubSystemDaoTest
-- 使用唯一前缀 svc-subsys-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_portal" ("code", "name", "remark", "active", "built_in")
    values ('svc-portal-subsys-dao-test-1', 'svc-portal-subsys-dao-test-1-name', 'from SysSubSystemDaoTest', true, false);

merge into "sys_sub_system" ("code", "name", "portal_code", "remark", "active", "built_in")
    values ('svc-subsys-dao-test-1', 'svc-subsys-dao-test-1-name', 'svc-portal-subsys-dao-test-1', 'from SysSubSystemDaoTest', true, false),
           ('svc-subsys-dao-test-2', 'svc-subsys-dao-test-2-name', 'svc-portal-subsys-dao-test-1', 'from SysSubSystemDaoTest', true, false);
