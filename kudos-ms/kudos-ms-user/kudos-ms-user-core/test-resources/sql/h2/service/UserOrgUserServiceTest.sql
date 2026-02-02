-- user_account: 每条 id 唯一，使用 svc-orguser-test-* 前缀隔离
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('6cd22b48-0000-0000-0000-000000000060', 'svc-user-orgus-test-1-xYvfu9vP', 'svc-tenan-orgu-test-1-xYvfu9vP', 'encrypted-pwd-1-xYvfu9vP', '00000000-0000-0000-0000-000000000000', 'from UserOrgUserServiceTest', true, false, 'system', '系统'),
    ('6cd22b48-0000-0000-0000-000000000061', 'svc-user-orgus-test-2-xYvfu9vP', 'svc-tenan-orgu-test-1-xYvfu9vP', 'encrypted-pwd-2-xYvfu9vP', '00000000-0000-0000-0000-000000000000', 'from UserOrgUserServiceTest', true, false, 'system', '系统'),
    ('6cd22b48-0000-0000-0000-000000000062', 'svc-user-orgus-test-3-xYvfu9vP', 'svc-tenan-orgu-test-1-xYvfu9vP', 'encrypted-pwd-3-xYvfu9vP', '00000000-0000-0000-0000-000000000000', 'from UserOrgUserServiceTest', true, false, 'system', '系统');

-- user_org: 每条 id 唯一
merge into "user_org" ("id", "name", "tenant_id", "org_type_dict_code", "sort_num", "remark", "active", "built_in", "create_user_id", "create_user_name") values
    ('6cd22b48-0000-0000-0000-000000000063', 'svc-org-orgus-test-1-xYvfu9vP', 'svc-tenan-orgu-test-1-xYvfu9vP', 'ORG_TYPE_TEST', 1, 'from UserOrgUserServiceTest', true, false, 'system', '系统'),
    ('6cd22b48-0000-0000-0000-000000000064', 'svc-org-orgus-test-2-xYvfu9vP', 'svc-tenan-orgu-test-1-xYvfu9vP', 'ORG_TYPE_TEST', 2, 'from UserOrgUserServiceTest', true, false, 'system', '系统');

-- user_org_user: 已存在关系供 exists/unbind 用例使用
merge into "user_org_user" ("id", "org_id", "user_id", "org_admin", "create_user_id", "create_user_name") values
    ('6cd22b48-0000-0000-0000-000000000065', '6cd22b48-0000-0000-0000-000000000063', '6cd22b48-0000-0000-0000-000000000060', true, 'system', '系统'),
    ('6cd22b48-0000-0000-0000-000000000066', '6cd22b48-0000-0000-0000-000000000063', '6cd22b48-0000-0000-0000-000000000061', false, 'system', '系统');
