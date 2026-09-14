--region DML
insert into "sys_cache" ("name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time", "ttl", "remark", "built_in", "hash")
values ('SYS_OUT_LINE_BY_SYSTEM_AND_TENANT', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '出网白名单缓存(by systemCode + tenantId)', true, false);
--endregion DML
