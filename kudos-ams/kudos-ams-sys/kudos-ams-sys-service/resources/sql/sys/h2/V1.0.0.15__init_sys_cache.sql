--region DDL
create table if not exists "sys_cache"
(
    "id"                  char(36)  default RANDOM_UUID() not null primary key,
    "name"                character varying(64)           not null,
    "atomic_service_code" character varying(32)           not null,
    "strategy_dict_code"  character varying(16)           not null,
    "write_on_boot"       boolean   default FALSE         not null,
    "write_in_time"       boolean   default FALSE         not null,
    "ttl"                 int2,
    "remark"              character varying(128),
    "active"              boolean   default TRUE          not null,
    "built_in"            boolean   default FALSE         not null,
    "create_user"         character varying(36),
    "create_time"         timestamp default now(),
    "update_user"         character varying(36),
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
comment on column "sys_cache"."create_user" is '创建用户';
comment on column "sys_cache"."create_time" is '创建时间';
comment on column "sys_cache"."update_user" is '更新用户';
comment on column "sys_cache"."update_time" is '更新时间';
--endregion DDL


--region DML
insert into "sys_cache" ("id", "name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time",
                         "ttl", "remark", "active", "built_in")
values ('14a9adc4-6bb5-45bd-96bb-d8afe3060bea', 'SYS_DOMAIN', 'ms-sys', 'LOCAL_REMOTE', true, true, 3600,
        '域名缓存（内置）', true, true),
       ('654f5484-13b0-46f1-a2d7-4734e8effdf7', 'SYS_SUB_SYSTEM', 'ms-sys', 'LOCAL_REMOTE', true, false, 900,
        '子系统缓存（内置）', true, true),
       ('0e62a3ff-ccbd-42b8-86cc-ed5b4337ce5a', 'SYS_PARAM', 'ms-sys', 'LOCAL_REMOTE', true, false, 3600,
        '参数缓存（内置）', true, true),
       ('2942ecce-2849-4edb-8f9b-68d5979e466d', 'SYS_IP_ACCESS_RULE', 'ms-sys', 'LOCAL_REMOTE', true, false, 300,
        'IP访问规则（内置）', true, true),
       ('b690d885-7ca0-40f6-81ca-36b33db2e157', 'SYS_MODULE', 'ms-sys', 'LOCAL_REMOTE', true, false, 3600,
        '模块缓存（内置）', true, true),
       ('3e32a62f-56c5-49eb-8df8-5dd21ee4c818', 'CONTROLLER_CACHE', 'ms-sys', 'LOCAL_REMOTE', false, false, 900,
        'web层缓存（内置）', true, true),
       ('e50678db-eb4b-4d1c-b12f-057f1598353f', 'SYS_CACHE', 'ms-sys', 'LOCAL_REMOTE', false, false, 900,
        '缓存配置（内置）', true, true),
       ('63ae2697-2e4b-4aa7-a3ba-5cd00b773d5d', 'SYS_CONTEXT_SUB_CODE', 'ms-sys', 'LOCAL_REMOTE', false, false, 900,
        '上下子系统通行（内置）', true, true),
       ('ec895b20-c284-460a-a971-e97bfc3a058f', 'TENANT_DATASOURCE', 'ms-sys', 'LOCAL_REMOTE', false, false, 300,
        '子系統數據源（内置）', true, true),
       ('84ec0b11-f681-4c76-823b-9ef9403362a8', 'SYS_DICT', 'ms-sys', 'LOCAL_REMOTE', false, false, 900,
        '系统字典缓存（内置）', true, true),
       ('eec32f26-75f9-4400-ac81-4c198978d4be', 'SYS_I18N', 'ms-sys', 'LOCAL_REMOTE', false, false, 3600,
        '国际化缓存（内置）', true, true),
       ('e5340806-97b4-43a4-84c6-97e5e2966371', 'TENANT_LANGUAGE', 'ms-sys', 'LOCAL_REMOTE', false, false, 900,
        '租户语言缓存', true, true),
       ('2da8e352-6e6f-4cd4-93e0-259ad3c7ea83', 'SYS_DATASOURCE', 'ms-sys', 'LOCAL_REMOTE', true, true, 1800,
        '数据源缓存', true, true);
--endregion DML
