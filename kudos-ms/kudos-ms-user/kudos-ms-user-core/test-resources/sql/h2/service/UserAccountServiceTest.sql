-- 测试数据：UserAccountServiceTest
-- 使用唯一前缀 svc-user-test-* 和唯一UUID确保测试数据隔离

merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "org_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('a970f8c0-0000-0000-0000-000000000016', 'svc-user-test-1-3iZR7Pv6', 'svc-tenant-user-test-1-3iZR7Pv6', 'encrypted-pwd-1-3iZR7Pv6', '00000000-0000-0000-0000-000000000000', 'a970f8c0-0000-0000-0000-000000000020', 'from UserAccountServiceTest', true, false, 'system', '系统'),
           ('a970f8c0-0000-0000-0000-000000000017', 'svc-user-test-2-3iZR7Pv6', 'svc-tenant-user-test-1-3iZR7Pv6', 'encrypted-pwd-2-3iZR7Pv6', 'a970f8c0-0000-0000-0000-000000000016', 'a970f8c0-0000-0000-0000-000000000020', 'from UserAccountServiceTest', true, false, 'system', '系统'),
           ('a970f8c0-0000-0000-0000-000000000018', 'svc-user-test-3-3iZR7Pv6', 'svc-tenant-user-test-1-3iZR7Pv6', 'encrypted-pwd-3-3iZR7Pv6', 'a970f8c0-0000-0000-0000-000000000016', 'a970f8c0-0000-0000-0000-000000000021', 'from UserAccountServiceTest', true, false, 'system', '系统'),
           ('a970f8c0-0000-0000-0000-000000000019', 'svc-user-test-4-3iZR7Pv6', 'svc-tenant-user-test-2-3iZR7Pv6', 'encrypted-pwd-4-3iZR7Pv6', '00000000-0000-0000-0000-000000000000', null, 'from UserAccountServiceTest', false, false, 'system', '系统');

-- 创建测试用的机构数据（用于测试 getUsersByOrgId）
merge into "user_org" ("id", "name", "tenant_id", "org_type_dict_code", "sort_num", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('a970f8c0-0000-0000-0000-000000000020', 'svc-org-user-test-1-3iZR7Pv6', 'svc-tenant-user-test-1-3iZR7Pv6', 'ORG_TYPE_TEST', 1, 'from UserAccountServiceTest', true, false, 'system', '系统'),
           ('a970f8c0-0000-0000-0000-000000000021', 'svc-org-user-test-2-3iZR7Pv6', 'svc-tenant-user-test-1-3iZR7Pv6', 'ORG_TYPE_TEST', 2, 'from UserAccountServiceTest', true, false, 'system', '系统');
