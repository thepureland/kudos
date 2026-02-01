-- 测试数据：UserOrgServiceTest
-- 使用唯一前缀 svc-org-test-* 和唯一UUID确保测试数据隔离

merge into "user_org" ("id", "name", "short_name", "tenant_id", "parent_id", "org_type_dict_code", "sort_num", "remark", "active", "built_in", "create_user_id", "create_user_name")
    values ('8b4df430-0000-0000-0000-000000000030', 'svc-org-test-root-1-HuAyup4R', 'svc-org-test-root-1-HuAyup4R', 'svc-tenant-org-test-1-HuAyup4R', null, 'ORG_TYPE_TEST', 1, 'from UserOrgServiceTest', true, false, 'system', '系统'),
           ('8b4df430-0000-0000-0000-000000000031', 'svc-org-test-child-1-HuAyup4R', 'svc-org-test-child-1-HuAyup4R', 'svc-tenant-org-test-1-HuAyup4R', '8b4df430-0000-0000-0000-000000000030', 'ORG_TYPE_TEST', 11, 'from UserOrgServiceTest', true, false, 'system', '系统'),
           ('8b4df430-0000-0000-0000-000000000032', 'svc-org-test-child-2-HuAyup4R', 'svc-org-test-child-2-HuAyup4R', 'svc-tenant-org-test-1-HuAyup4R', '8b4df430-0000-0000-0000-000000000030', 'ORG_TYPE_TEST', 12, 'from UserOrgServiceTest', true, false, 'system', '系统'),
           ('8b4df430-0000-0000-0000-000000000033', 'svc-org-test-grandchild-1-HuAyup4R', 'svc-org-test-grch-1-HuAyup4R', 'svc-tenant-org-test-1-HuAyup4R', '8b4df430-0000-0000-0000-000000000031', 'ORG_TYPE_TEST', 111, 'from UserOrgServiceTest', true, false, 'system', '系统'),
           ('8b4df430-0000-0000-0000-000000000034', 'svc-org-test-root-2-HuAyup4R', 'svc-org-test-root-2-HuAyup4R', 'svc-tenant-org-test-1-HuAyup4R', null, 'ORG_TYPE_TEST', 2, 'from UserOrgServiceTest', true, false, 'system', '系统'),
           ('8b4df430-0000-0000-0000-000000000035', 'svc-org-test-inactive', 'svc-org-test-inactive', 'svc-tenant-org-test-1-HuAyup4R', null, 'ORG_TYPE_TEST', 3, 'from UserOrgServiceTest', false, false, 'system', '系统'),
           ('8b4df430-0000-0000-0000-000000000036', 'svc-org-test-tenant2-HuAyup4R', 'svc-org-test-tenant2-HuAyup4R', 'svc-tenant-org-test-2-HuAyup4R', null, 'ORG_TYPE_TEST', 1, 'from UserOrgServiceTest', true, false, 'system', '系统');
