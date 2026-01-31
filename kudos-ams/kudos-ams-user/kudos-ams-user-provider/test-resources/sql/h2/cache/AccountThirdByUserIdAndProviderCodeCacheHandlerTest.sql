-- 测试数据 for AccountThirdByUserIdAndProviderCodeCacheHandler

merge into "user_account" ("id", "username", "tenant_id", "login_password", "display_name", "supervisor_id", "remark", "active", "built_in")
    values ('9a1a0000-0000-0000-0000-000000000001', 'third_user1', 'tenant-third-1', 'password', '第三方用户1', '00000000-0000-0000-0000-000000000000', '测试用户1', true, false),
           ('9a1a0000-0000-0000-0000-000000000002', 'third_user2', 'tenant-third-1', 'password', '第三方用户2', '00000000-0000-0000-0000-000000000000', '测试用户2', true, false);

merge into "user_account_third" ("id", "user_id", "account_provider_dict_code", "account_provider_issuer", "subject", "union_id", "external_display_name", "external_email", "avatar_url", "last_login_time", "tenant_id", "remark", "active", "built_in")
    values ('9b1a0000-0000-0000-0000-000000000001', '9a1a0000-0000-0000-0000-000000000001', 'WX', null, 'sub-1', 'union-1', 'wx-user1', 'wx1@example.com', null, null, 'tenant-third-1', 'wx', true, false),
           ('9b1a0000-0000-0000-0000-000000000002', '9a1a0000-0000-0000-0000-000000000001', 'QQ', null, 'sub-2', 'union-2', 'qq-user1', 'qq1@example.com', null, null, 'tenant-third-1', 'qq', true, false),
           ('9b1a0000-0000-0000-0000-000000000003', '9a1a0000-0000-0000-0000-000000000001', 'GITHUB', null, 'sub-3', 'union-3', 'gh-user1', 'gh1@example.com', null, null, 'tenant-third-1', 'gh', false, false),
           ('9b1a0000-0000-0000-0000-000000000004', '9a1a0000-0000-0000-0000-000000000002', 'WX', null, 'sub-4', 'union-4', 'wx-user2', 'wx2@example.com', null, null, 'tenant-third-1', 'wx', true, false);
