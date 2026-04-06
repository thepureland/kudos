-- user_account: 每条 id 唯一，供第三方账号关联
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "org_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('11111111-0000-0000-0000-000000000001', 'third-user-1', 'tenant-third-test-1', 'encrypted-pwd-1', '00000000-0000-0000-0000-000000000000', 'org-001', 'from UserAccountThirdDaoTest', true, false, 'system', '系统');

-- user_account_third: 每条 id 唯一
merge into "user_account_third" ("id", "user_id", "account_provider_dict_code", "account_provider_issuer", "subject", "union_id", "external_display_name", "external_email", "avatar_url", "last_login_time", "tenant_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('22222222-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001', 'github', 'https://github.com', 'github-user-001', 'union-001', 'GitHub User 1', 'user1@example.com', 'https://avatar.example.com/1', CURRENT_TIMESTAMP, 'tenant-third-test-1', 'from UserAccountThirdDaoTest', true, false, 'system', '系统');
