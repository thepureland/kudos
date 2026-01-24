-- 测试数据：SysSubSystemMicroServiceDaoTest
-- 使用唯一前缀 svc-subsysms-dao-test-* 和唯一UUID确保测试数据隔离

merge into "sys_portal" ("code", "name", "remark", "active", "built_in")
    values ('svc-portal-subsysms-dao-test-1', 'svc-portal-subsysms-dao-test-1-name', 'from SysSubSystemMicroServiceDaoTest', true, false);

merge into "sys_sub_system" ("code", "name", "portal_code", "remark", "active", "built_in")
    values ('svc-subsys-subsysms-dao-test-1', 'svc-subsys-subsysms-dao-test-1-name', 'svc-portal-subsysms-dao-test-1', 'from SysSubSystemMicroServiceDaoTest', true, false),
           ('svc-subsys-subsysms-dao-test-2', 'svc-subsys-subsysms-dao-test-2-name', 'svc-portal-subsysms-dao-test-1', 'from SysSubSystemMicroServiceDaoTest', true, false);

merge into "sys_micro_service" ("code", "name", "context", "remark", "active", "built_in")
    values ('svc-ms-subsysms-dao-test-1', 'svc-ms-subsysms-dao-test-1-name', '/svc-ms-ssms-dao-test-1', 'from SysSubSystemMicroServiceDaoTest', true, false),
           ('svc-ms-subsysms-dao-test-2', 'svc-ms-subsysms-dao-test-2-name', '/svc-ms-ssms-dao-test-2', 'from SysSubSystemMicroServiceDaoTest', true, false);

merge into "sys_sub_system_micro_service" ("id", "sub_system_code", "micro_service_code")
    values ('40000000-0000-0000-0000-000000000100', 'svc-subsys-subsysms-dao-test-1', 'svc-ms-subsysms-dao-test-1'),
           ('40000000-0000-0000-0000-000000000101', 'svc-subsys-subsysms-dao-test-1', 'svc-ms-subsysms-dao-test-2'),
           ('40000000-0000-0000-0000-000000000102', 'svc-subsys-subsysms-dao-test-2', 'svc-ms-subsysms-dao-test-1');
