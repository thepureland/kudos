-- 测试数据：UserOrgDaoTest
-- 使用唯一前缀 user-org-dao-test-* 和唯一UUID确保测试数据隔离

merge into "user_org" ("id", "name", "tenant_id", "org_type_dict_code", "sort_num", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('669d8a45-0000-0000-0000-000000000010', 'user-org-dao-test-1-w2nwgBMU', 'user-tenant-dao-test-1-w2nwgBMU', 'ORG_TYPE_TEST', 1, 'from UserOrgDaoTest', true, false, 'system', '系统'),
           ('669d8a45-0000-0000-0000-000000000011', 'user-org-dao-test-2-w2nwgBMU', 'user-tenant-dao-test-1-w2nwgBMU', 'ORG_TYPE_TEST', 2, 'from UserOrgDaoTest', true, false, 'system', '系统'),
           ('669d8a45-0000-0000-0000-000000000012', 'user-org-dao-test-3-w2nwgBMU', 'user-tenant-dao-test-2-w2nwgBMU', 'ORG_TYPE_TEST', 3, 'from UserOrgDaoTest', false, false, 'system', '系统');