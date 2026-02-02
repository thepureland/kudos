-- user_account: 每条 id 唯一，供登录日志 user_id 关联
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('1a14e2ae-0000-0000-0000-000000000078', 'svc-user-loglog-test-1-88Sexq53', 'svc-tenan-loglo-test-1-88Sexq53', 'encrypted-pwd-1-88Sexq53', '00000000-0000-0000-0000-000000000000', 'from UserLogLoginServiceTest', true, false, 'system', '系统'),
    ('1a14e2ae-0000-0000-0000-000000000079', 'svc-user-loglog-test-2-88Sexq53', 'svc-tenan-loglo-test-1-88Sexq53', 'encrypted-pwd-2-88Sexq53', '00000000-0000-0000-0000-000000000000', 'from UserLogLoginServiceTest', true, false, 'system', '系统'),
    ('1a14e2ae-0000-0000-0000-000000000080', 'svc-user-loglog-test-3-88Sexq53', 'svc-tenan-loglo-test-2-88Sexq53', 'encrypted-pwd-3-88Sexq53', '00000000-0000-0000-0000-000000000000', 'from UserLogLoginServiceTest', true, false, 'system', '系统');

-- user_log_login: 每条 id 唯一，user_id 关联 user_account
merge into "user_log_login" ("id", "user_id", "username", "tenant_id", "login_time", "login_ip", "login_success", "failure_reason", "create_time") values
    ('1a14e2ae-0000-0000-0000-000000000070', '1a14e2ae-0000-0000-0000-000000000078', 'svc-user-loglog-test-1-88Sexq53', 'svc-tenan-loglo-test-1-88Sexq53', '2024-01-01 10:00:00', 192168001001, true, null, '2024-01-01 10:00:00'),
    ('1a14e2ae-0000-0000-0000-000000000071', '1a14e2ae-0000-0000-0000-000000000078', 'svc-user-loglog-test-1-88Sexq53', 'svc-tenan-loglo-test-1-88Sexq53', '2024-01-02 10:00:00', 192168001002, true, null, '2024-01-02 10:00:00'),
    ('1a14e2ae-0000-0000-0000-000000000072', '1a14e2ae-0000-0000-0000-000000000078', 'svc-user-loglog-test-1-88Sexq53', 'svc-tenan-loglo-test-1-88Sexq53', '2024-01-03 10:00:00', 192168001003, false, '密码错误', '2024-01-03 10:00:00'),
    ('1a14e2ae-0000-0000-0000-000000000073', '1a14e2ae-0000-0000-0000-000000000079', 'svc-user-loglog-test-2-88Sexq53', 'svc-tenan-loglo-test-1-88Sexq53', '2024-01-04 10:00:00', 192168001004, true, null, '2024-01-04 10:00:00'),
    ('1a14e2ae-0000-0000-0000-000000000074', '1a14e2ae-0000-0000-0000-000000000079', 'svc-user-loglog-test-2-88Sexq53', 'svc-tenan-loglo-test-1-88Sexq53', '2024-01-05 10:00:00', 192168001005, false, '账户被锁定', '2024-01-05 10:00:00'),
    ('1a14e2ae-0000-0000-0000-000000000075', '1a14e2ae-0000-0000-0000-000000000080', 'svc-user-loglog-test-3-88Sexq53', 'svc-tenan-loglo-test-2-88Sexq53', '2024-01-06 10:00:00', 192168001006, true, null, '2024-01-06 10:00:00'),
    ('1a14e2ae-0000-0000-0000-000000000076', '1a14e2ae-0000-0000-0000-000000000078', 'svc-user-loglog-test-1-88Sexq53', 'svc-tenan-loglo-test-1-88Sexq53', '2024-01-10 10:00:00', 192168001010, true, null, '2024-01-10 10:00:00'),
    ('1a14e2ae-0000-0000-0000-000000000077', '1a14e2ae-0000-0000-0000-000000000078', 'svc-user-loglog-test-1-88Sexq53', 'svc-tenan-loglo-test-1-88Sexq53', '2024-01-11 10:00:00', 192168001011, true, null, '2024-01-11 10:00:00');
