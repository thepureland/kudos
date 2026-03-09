--region DML
insert into "sys_cache" ("name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time", "ttl", "remark", "hash") values 
   ('USER_BY_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户缓存(by id)', false),
   ('USER_ID_BY_TENANT_ID_AND_USERNAME', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户ID缓存(by tenantId & username)', false),
   ('ACCOUNT_THIRD_BY_USER_ID_AND_PROVIDER_CODE', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '第三方账号缓存(by userId & providerCode)', false),
   ('USER_IDS_BY_ORG_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户ID列表缓存(by orgId)', false),
   ('USER_CONTACT_WAY_BY_USER_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户联系方式缓存(by userId)', false),
   ('REMEMBER_ME_BY_TENANT_ID_AND_USERNAME', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '记住我登录缓存(by tenantId & username)', false),
   ('USER_ORG_IDS_BY_USER_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '机构ID列表缓存(by userId)', false),
   ('USER_ORG__HASH', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '机构Hash缓存', true),
   ('USER_ACCOUNT__HASH', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户Hash缓存', true);

--endregion DML
