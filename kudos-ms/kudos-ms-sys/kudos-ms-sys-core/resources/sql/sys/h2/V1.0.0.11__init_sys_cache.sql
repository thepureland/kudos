--region DDL
create table if not exists "sys_cache"
(
    "id"                  char(36)  default RANDOM_UUID() not null primary key,
    "name"                character varying(64)           not null,
    "atomic_service_code" character varying(32)           not null,
    "strategy_dict_code"  character varying(16)           not null,
    "write_on_boot"       boolean   default FALSE         not null,
    "write_in_time"       boolean   default FALSE         not null,
    "ttl"                 int4,
    "remark"              character varying(128),
    "active"              boolean   default TRUE          not null,
    "built_in"            boolean   default FALSE         not null,
    "hash"                boolean   default FALSE         not null,
    "create_user_id"      character varying(36),
    "create_user_name"    character varying(32),
    "create_time"         timestamp(6) default now(),
    "update_user_id"      character varying(36),
    "update_user_name"    character varying(32),
    "update_time"         timestamp(6)
);

create unique index if not exists "uq_sys_cache" on "sys_cache" ("name", "atomic_service_code");

comment on table "sys_cache" is '缓存';
comment on column "sys_cache"."id" is '主键';
comment on column "sys_cache"."name" is '名称';
comment on column "sys_cache"."atomic_service_code" is '原子服务编码';
comment on column "sys_cache"."strategy_dict_code" is '缓存策略代码';
comment on column "sys_cache"."write_on_boot" is '是否启动时写缓存';
comment on column "sys_cache"."write_in_time" is '是否及时回写缓存';
comment on column "sys_cache"."ttl" is '缓存生存时间(秒)';
comment on column "sys_cache"."remark" is '备注';
comment on column "sys_cache"."active" is '是否启用';
comment on column "sys_cache"."built_in" is '是否内置';
comment on column "sys_cache"."hash" is '是否Hash缓存';
comment on column "sys_cache"."create_user_id" is '创建者id';
comment on column "sys_cache"."create_user_name" is '创建者名称';
comment on column "sys_cache"."create_time" is '创建时间';
comment on column "sys_cache"."update_user_id" is '更新者id';
comment on column "sys_cache"."update_user_name" is '更新者名称';
comment on column "sys_cache"."update_time" is '更新时间';
--endregion DDL


--region DML
insert into "sys_cache" ("name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time", "ttl", "remark", "hash") values
    ('SYS_CACHE_BY_NAME', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '缓存配置信息的缓存(by name)', false),
    ('SYS_DICT_BY_ID', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '字典缓存(by id)', false),
    ('SYS_DICT_ITEMS_BY_MS_CODE_AND_TYPE', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '字典项缓存(by atomicServiceCode & dictType)', false),
    ('SYS_DOMAIN_BY_NAME', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '域名缓存(by name)', false),
    ('SYS_PARAM_BY_MODULE_AND_NAME', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '参数缓存(by atomicServiceCode & name)', false),
    ('SYS_TENANT_BY_ID', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '租户缓存(by id)', false),
    ('SYS_TENANT_SYSTEM__HASH', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '租户-系统关系缓存', true),
    ('SYS_ATOMIC_SERVICE_BY_CODE', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '原子服务缓存(by code)', false),
    ('I18N_BY_LOCALE_AND_TYPE_AND_MS_CODE', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '国际化信息缓存(by locale & type & atomicServiceCode)', false),
    ('SYS_I18N__HASH', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '国际化信息Hash缓存', true),
    ('SYS_MODULE_BY_CODE', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '模块缓存(by code)', false),
    ('SYS_ACCESS_RULE_IPS_BY_SYSTEM_CODE_AND_TENANT_ID', 'sys', 'LOCAL_REMOTE', true, true, 999999999, 'ip访问规则缓存(by subSystemCode & tenantId)', false),
    ('SYS_RESOURCE__HASH', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '资源Hash缓存', true),
    ('SYS_SYSTEM__HASH', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '系统Hash缓存', true),
    ('SYS_DATA_SOURCE__HASH', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '数据源Hash缓存', true),
    ('SYS_MICRO_SERVICE__HASH', 'sys', 'LOCAL_REMOTE', true, true, 999999999, '微服务Hash缓存', true);

--endregion DML
