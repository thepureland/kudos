--region DML
insert into "sys_cache" ("name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time", "ttl", "remark", "built_in", "hash")
values ('SYS_LOCALE_BY_CODE', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '语言/区域字典缓存(by code)', true, false);
--endregion DML
