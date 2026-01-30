-- 测试数据：UserAccountDaoTest
-- 使用唯一前缀 user-account-dao-test-* 和唯一UUID确保测试数据隔离

merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('c07f7328-0000-0000-0000-000000000001', 'user-account-dao-test-1-Y6hpgGno', 'user-tenant-dao-test-1-Y6hpgGno', 'encrypted-pwd-1-Y6hpgGno', '00000000-0000-0000-0000-000000000000', 'from UserAccountDaoTest', true, false, 'system', '系统'),
           ('c07f7328-0000-0000-0000-000000000002', 'user-account-dao-test-2-Y6hpgGno', 'user-tenant-dao-test-1-Y6hpgGno', 'encrypted-pwd-2-Y6hpgGno', 'c07f7328-0000-0000-0000-000000000001', 'from UserAccountDaoTest', true, false, 'system', '系统'),
           ('c07f7328-0000-0000-0000-000000000003', 'user-account-dao-test-3-Y6hpgGno', 'user-tenant-dao-test-2-Y6hpgGno', 'encrypted-pwd-3-Y6hpgGno', '00000000-0000-0000-0000-000000000000', 'from UserAccountDaoTest', false, false, 'system', '系统');