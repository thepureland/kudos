-- 测试数据：SysSubSystemMicroServiceDaoTest（每条 id 唯一）

merge into "sys_system" ("code", "name", "parent_code", "sub_system", "remark", "active", "built_in") values
    ('svc-system-subsysms-dao-tes_1506', 'svc-system-subsysms-dao-tes_1506-name', null, false, 'from SysSubSystemMicroServiceDaoTest', true, false),
    ('svc-subsys-subsysms-dao-tes_1506', 'svc-subsys-subsysms-dao-tes_1506-name', 'svc-system-subsysms-dao-tes_1506', true, 'from SysSubSystemMicroServiceDaoTest', true, false),
    ('svc-subsys-subsysms-dao-test-1', 'svc-subsys-subsysms-dao-test-1-name', 'svc-system-subsysms-dao-tes_1506', true, 'from SysSubSystemMicroServiceDaoTest', true, false);

merge into "sys_micro_service" ("code", "name", "context", "remark", "active", "built_in") values
    ('svc-ms-subsysms-dao-test-1_1506', 'svc-ms-subsysms-dao-test-1_1506-name', '/svc-ms-ssms-dao-test-1', 'from SysSubSystemMicroServiceDaoTest', true, false),
    ('svc-ms-subsysms-dao-test-2_1506', 'svc-ms-subsysms-dao-test-2_1506-name', '/svc-ms-ssms-dao-test-2', 'from SysSubSystemMicroServiceDaoTest', true, false);

merge into "sys_sub_system_micro_service" ("id", "sub_system_code", "micro_service_code") values
    ('40000000-0000-0000-0000-000000001506', 'svc-subsys-subsysms-dao-test-1', 'svc-ms-subsysms-dao-test-1_1506'),
    ('40000000-0000-0000-0000-000000001507', 'svc-subsys-subsysms-dao-test-1', 'svc-ms-subsysms-dao-test-2_1506'),
    ('40000000-0000-0000-0000-000000001508', 'svc-subsys-subsysms-dao-tes_1506', 'svc-ms-subsysms-dao-test-1_1506');
