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
    "create_user_id"      character varying(36),
    "create_user_name"    character varying(32),
    "create_time"         timestamp default now(),
    "update_user_id"      character varying(36),
    "update_user_name"    character varying(32),
    "update_time"         timestamp
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
comment on column "sys_cache"."create_user_id" is '创建者id';
comment on column "sys_cache"."create_user_name" is '创建者名称';
comment on column "sys_cache"."create_time" is '创建时间';
comment on column "sys_cache"."update_user_id" is '更新者id';
comment on column "sys_cache"."update_user_name" is '更新者名称';
comment on column "sys_cache"."update_time" is '更新时间';
--endregion DDL


--region DML
insert into "sys_cache" ("id", "name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time",
                         "ttl", "remark", "active", "built_in")
values ('14a9adc4-6bb5-45bd-96bb-d8afe3060bea', 'SYS_CACHE_BY_NAME', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '缓存配置信息的缓存(by name)', true, true),
       ('654f5484-13b0-46f1-a2d7-4734e8effdf7', 'SYS_DATA_SOURCE_BY_ID', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '数据源缓存(by id)', true, true),
       ('0e62a3ff-ccbd-42b8-86cc-ed5b4337ce5a', 'SYS_DATA_SOURCE_BY_TENANT_ID_AND_3_CODES', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '数据源缓存(by subSystemCode & tenantId)', true, true),
       ('2942ecce-2849-4edb-8f9b-68d5979e466d', 'SYS_DICT_BY_ID', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '字典缓存(by id)', true, true),
       ('b690d885-7ca0-40f6-81ca-36b33db2e157', 'SYS_DICT_ITEMS_BY_MODULE_AND_TYPE', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '字典项缓存(by moduleCode & dictType)', true, true),
       ('3e32a62f-56c5-49eb-8df8-5dd21ee4c818', 'SYS_DOMAIN_BY_NAME', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '域名缓存(by name)', true, true),
       ('e50678db-eb4b-4d1c-b12f-057f1598353f', 'SYS_PARAM_BY_MODULE_AND_NAME', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '参数缓存(by moduleCode & name)', true, true),
       ('ec895b20-c284-460a-a971-e97bfc3a058f', 'SYS_RESOURCE_BY_ID', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '资源缓存(by id)', true, true),
       ('84ec0b11-f681-4c76-823b-9ef9403362a8', 'SYS_RESOURCE_ID_BY_SUB_SYS_AND_URL', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '资源缓存(by subSystemCode & url)', true, true),
       ('eec32f26-75f9-4400-ac81-4c198978d4be', 'SYS_RESOURCE_IDS_BY_SUB_SYS_AND_TYPE', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '资源缓存(by subSystemCode & type)', true, true),
       ('e5340806-97b4-43a4-84c6-97e5e2966371', 'SYS_TENANT_BY_ID', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '租户缓存(by id)', true, true),
       ('2da8e352-6e6f-4cd4-93e0-259ad3c7ea83', 'SYS_TENANT_IDS_BY_SUB_SYS', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '租户缓存(by subSystemCode)', true, true),
       ('3da8e352-1e6f-4cd4-13e0-259ad3c7ea81', 'SYS_PORTAL_BY_CODE', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '门户缓存(by code)', true, true),
       ('4da8e351-2e6f-3cd4-23e0-359ad3c7ea82', 'SYS_SUB_SYSTEM_BY_CODE', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '子系统缓存(by code)', true, true),
       ('5da83351-3e6f-3cd4-33e0-359ad3c7ea84', 'SYS_MICRO_SERVICE_BY_CODE', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '微服务缓存(by code)', true, true),
       ('6da93351-336f-32d4-33e0-359ad3c7ea85', 'SYS_ATOMIC_SERVICE_BY_CODE', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '原子服务缓存(by code)', true, true),
       ('7da93321-236f-12d4-13e0-359ad3c7ea86', 'SYS_MODULE_BY_CODE', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        '模块缓存(by code)', true, true),
       ('8da92321-206f-12d4-23e0-359ad3c7ea87', 'SYS_ACCESS_RULE_IPS_BY_SUB_SYS_AND_TENANT_ID', 'ams-sys', 'LOCAL_REMOTE', true, true, 999999999,
        'ip访问规则缓存(by subSystemCode & tenantId)', true, true);

--endregion DML
