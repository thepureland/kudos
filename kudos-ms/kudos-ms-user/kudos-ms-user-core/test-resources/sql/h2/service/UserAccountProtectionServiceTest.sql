-- user_account: 每条 id 唯一，供账号保护关联
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "org_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('66666666-0000-0000-0000-000000000001', 'protect-user-1', 'tenant-protect-test-1', 'encrypted-pwd-1', '00000000-0000-0000-0000-000000000000', 'org-001', 'from UserAccountProtectionServiceTest', true, false, 'system', '系统');

-- user_account_protection: 每条 id 唯一，user_id 关联 user_account
merge into "user_account_protection" ("id", "user_id", "question1", "answer1", "question2", "answer2", "question3", "answer3", "safe_contact_way_id", "total_validate_count", "match_question_count", "error_times", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('66666666-0000-0000-0000-000000000101', '66666666-0000-0000-0000-000000000001', 'q1', 'a1', 'q2', 'a2', 'q3', 'a3', null, 2, 1, 0, 'from UserAccountProtectionServiceTest', true, false, 'system', '系统');
