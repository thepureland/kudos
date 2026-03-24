--region DML
insert into "sys_cache" ("name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time", "ttl", "remark", "built_in", "hash") values 
   ('USER_BY_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户缓存(by id)', true, false),
   ('USER_ID_BY_TENANT_ID_AND_USERNAME', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户ID缓存(by tenantId & username)', true, false),
   ('ACCOUNT_THIRD_BY_USER_ID_AND_PROVIDER_CODE', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '第三方账号缓存(by userId & providerCode)', true, false),
   ('USER_IDS_BY_ORG_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户ID列表缓存(by orgId)', true, false),
   ('USER_CONTACT_WAY_BY_USER_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户联系方式缓存(by userId)', true, false),
   ('REMEMBER_ME_BY_TENANT_ID_AND_USERNAME', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '记住我登录缓存(by tenantId & username)', true, false),
   ('USER_ORG_IDS_BY_USER_ID', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '机构ID列表缓存(by userId)', true, false),
   ('USER_ORG__HASH', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '机构Hash缓存', true, true),
   ('USER_ACCOUNT__HASH', 'auth', 'LOCAL_REMOTE', true, true, 999999999, '用户Hash缓存', true, true);

--endregion DML
