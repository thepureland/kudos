-- 测试数据：UserOrgUserDaoTest
-- 使用唯一前缀 user-orguser-dao-test-* 和唯一UUID确保测试数据隔离

-- 创建测试用的用户
merge into "user_account" ("id", "username", "tenant_id", "login_password", "supervisor_id", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('952bb1b3-0000-0000-0000-000000000040', 'user-account-dao-tes-1-KzzBoTkf', 'user-tena-dep-dao-tes-1-KzzBoTkf', 'encrypted-pwd-1-KzzBoTkf', '00000000-0000-0000-0000-000000000000', 'from UserOrgUserDaoTest', true, false, 'system', '系统'),
           ('952bb1b3-0000-0000-0000-000000000041', 'user-account-dao-tes-2-KzzBoTkf', 'user-tena-dep-dao-tes-1-KzzBoTkf', 'encrypted-pwd-2-KzzBoTkf', '00000000-0000-0000-0000-000000000000', 'from UserOrgUserDaoTest', true, false, 'system', '系统');

-- 创建测试用的机构
merge into "user_org" ("id", "name", "tenant_id", "org_type_dict_code", "sort_num", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('952bb1b3-0000-0000-0000-000000000042', 'user-org-dep-dao-tes-1-KzzBoTkf', 'user-tena-dep-dao-tes-1-KzzBoTkf', 'ORG_TYPE_TEST', 1, 'from UserOrgUserDaoTest', true, false, 'system', '系统'),
           ('952bb1b3-0000-0000-0000-000000000043', 'user-org-dep-dao-tes-2-KzzBoTkf', 'user-tena-dep-dao-tes-1-KzzBoTkf', 'ORG_TYPE_TEST', 2, 'from UserOrgUserDaoTest', true, false, 'system', '系统');

-- 创建已存在的机构-用户关系（用于测试exists）
merge into "user_org_user" ("id", "org_id", "user_id", "org_admin", "create_user_id", "create_user_name")
    values ('952bb1b3-0000-0000-0000-000000000044', '952bb1b3-0000-0000-0000-000000000042', '952bb1b3-0000-0000-0000-000000000040', true, 'system', '系统'),
           ('952bb1b3-0000-0000-0000-000000000045', '952bb1b3-0000-0000-0000-000000000042', '952bb1b3-0000-0000-0000-000000000041', false, 'system', '系统');