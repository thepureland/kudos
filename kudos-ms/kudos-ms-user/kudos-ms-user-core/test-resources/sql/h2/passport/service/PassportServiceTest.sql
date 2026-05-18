-- 测试数据：PassportServiceTest
-- 两条用户：一条 active=true，一条 active=false。login_password 在 @BeforeEach 中替换为 BCrypt 哈希。

merge into "user_account"
    ("id", "username", "tenant_id", "login_password", "supervisor_id", "org_id",
     "login_error_times", "active", "built_in", "create_user_id", "create_user_name")
values
    ('b970f8c0-0000-0000-0000-000000000001', 'svc-passport-test-active', 'svc-tenant-passport-test',
     'placeholder-overwritten-in-beforeeach', '00000000-0000-0000-0000-000000000000', null,
     0, true, false, 'system', '系统'),
    ('b970f8c0-0000-0000-0000-000000000002', 'svc-passport-test-inactive', 'svc-tenant-passport-test',
     'placeholder-overwritten-in-beforeeach', '00000000-0000-0000-0000-000000000000', null,
     0, false, false, 'system', '系统');
