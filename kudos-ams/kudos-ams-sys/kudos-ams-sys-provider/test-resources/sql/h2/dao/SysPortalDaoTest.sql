-- 测试数据：SysPortalDaoTest
-- 使用唯一前缀 svc-portal-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_portal" ("code", "name", "remark", "active", "built_in")
    values ('svc-portal-dao-test-1', 'svc-portal-dao-test-1-name', 'from SysPortalDaoTest', true, false),
           ('svc-portal-dao-test-2', 'svc-portal-dao-test-2-name', 'from SysPortalDaoTest', true, false);
