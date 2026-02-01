--region DML
insert into "sys_cache" ("id", "name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time",
                         "ttl", "remark", "active", "built_in")
values ('a1a1a1a1-1111-1111-1111-111111111101', 'USER_ORG_BY_ID', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '机构缓存(by id)', true, true),
       ('a1a1a1a1-1111-1111-1111-111111111102', 'USER_BY_ID', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '用户缓存(by id)', true, true),
       ('a1a1a1a1-1111-1111-1111-111111111104', 'USER_ID_BY_TENANT_ID_AND_USERNAME', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '用户ID缓存(by tenantId & username)', true, true),
       ('a1a1a1a1-1111-1111-1111-111111111105', 'ACCOUNT_THIRD_BY_USER_ID_AND_PROVIDER_CODE', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '第三方账号缓存(by userId & providerCode)', true, true),
       ('a1a1a1a1-1111-1111-1111-111111111106', 'USER_ORG_IDS_BY_TENANT_ID', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '机构ID列表缓存(by tenantId)', true, true),
       ('a1a1a1a1-1111-1111-1111-111111111107', 'USER_IDS_BY_ORG_ID', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '用户ID列表缓存(by orgId)', true, true),
       ('a1a1a1a1-1111-1111-1111-111111111108', 'USER_CONTACT_WAY_BY_USER_ID', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '用户联系方式缓存(by userId)', true, true),
       ('a1a1a1a1-1111-1111-1111-111111111109', 'REMEMBER_ME_BY_TENANT_ID_AND_USERNAME', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '记住我登录缓存(by tenantId & username)', true, true),
       ('a1a1a1a1-1111-1111-1111-111111111113', 'USER_ORG_IDS_BY_USER_ID', 'ams-auth', 'LOCAL_REMOTE', true, true, 999999999,
        '机构ID列表缓存(by userId)', true, true);

--endregion DML
