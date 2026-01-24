-- 测试数据：AuthLogLoginServiceTest
-- 使用唯一前缀 svc-loglogin-test-* 和唯一UUID确保测试数据隔离

-- 创建测试用的用户（用于登录日志的user_id关联）
merge into "auth_user" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('30000000-0000-0000-0000-000000000078', 'svc-user-loglogin-test-1', 'svc-tenant-loglogin-test-1', 'encrypted-pwd-1', '00000000-0000-0000-0000-000000000000', 'from AuthLogLoginServiceTest', true, false, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000079', 'svc-user-loglogin-test-2', 'svc-tenant-loglogin-test-1', 'encrypted-pwd-2', '00000000-0000-0000-0000-000000000000', 'from AuthLogLoginServiceTest', true, false, 'system', '系统'),
           ('30000000-0000-0000-0000-000000000080', 'svc-user-loglogin-test-3', 'svc-tenant-loglogin-test-2', 'encrypted-pwd-3', '00000000-0000-0000-0000-000000000000', 'from AuthLogLoginServiceTest', true, false, 'system', '系统');

merge into "auth_log_login" ("id", "user_id", "username", "tenant_id", "login_time", "login_ip", "login_success", "failure_reason", "create_time")
    values ('30000000-0000-0000-0000-000000000070', '30000000-0000-0000-0000-000000000078', 'svc-user-loglogin-test-1', 'svc-tenant-loglogin-test-1', '2024-01-01 10:00:00', 192168001001, true, null, '2024-01-01 10:00:00'),
           ('30000000-0000-0000-0000-000000000071', '30000000-0000-0000-0000-000000000078', 'svc-user-loglogin-test-1', 'svc-tenant-loglogin-test-1', '2024-01-02 10:00:00', 192168001002, true, null, '2024-01-02 10:00:00'),
           ('30000000-0000-0000-0000-000000000072', '30000000-0000-0000-0000-000000000078', 'svc-user-loglogin-test-1', 'svc-tenant-loglogin-test-1', '2024-01-03 10:00:00', 192168001003, false, '密码错误', '2024-01-03 10:00:00'),
           ('30000000-0000-0000-0000-000000000073', '30000000-0000-0000-0000-000000000079', 'svc-user-loglogin-test-2', 'svc-tenant-loglogin-test-1', '2024-01-04 10:00:00', 192168001004, true, null, '2024-01-04 10:00:00'),
           ('30000000-0000-0000-0000-000000000074', '30000000-0000-0000-0000-000000000079', 'svc-user-loglogin-test-2', 'svc-tenant-loglogin-test-1', '2024-01-05 10:00:00', 192168001005, false, '账户被锁定', '2024-01-05 10:00:00'),
           ('30000000-0000-0000-0000-000000000075', '30000000-0000-0000-0000-000000000080', 'svc-user-loglogin-test-3', 'svc-tenant-loglogin-test-2', '2024-01-06 10:00:00', 192168001006, true, null, '2024-01-06 10:00:00'),
           ('30000000-0000-0000-0000-000000000076', '30000000-0000-0000-0000-000000000078', 'svc-user-loglogin-test-1', 'svc-tenant-loglogin-test-1', '2024-01-10 10:00:00', 192168001010, true, null, '2024-01-10 10:00:00'),
           ('30000000-0000-0000-0000-000000000077', '30000000-0000-0000-0000-000000000078', 'svc-user-loglogin-test-1', 'svc-tenant-loglogin-test-1', '2024-01-11 10:00:00', 192168001011, true, null, '2024-01-11 10:00:00');
