--region DDL
create table if not exists "sys_tenant"
(
    "id"                    char(36)  default RANDOM_UUID() not null primary key,
    "name"                  character varying(128)          not null,
    "timezone"              character varying(16),
    "default_language_code" character varying(64),
    "remark"                character varying(256),
    "active"                boolean   default TRUE          not null,
    "built_in"              boolean   default FALSE         not null,
    "create_user_id"        character varying(36),
    "create_user_name"      character varying(32),
    "create_time"           timestamp default now(),
    "update_user_id"        character varying(36),
    "update_user_name"      character varying(32),
    "update_time"           timestamp
);


comment on table "sys_tenant" is '租户';
comment on column "sys_tenant"."id" is '主键';
comment on column "sys_tenant"."name" is '名称';
comment on column "sys_tenant"."timezone" is '时区';
comment on column "sys_tenant"."default_language_code" is '默认语言编码';
comment on column "sys_tenant"."remark" is '备注';
comment on column "sys_tenant"."active" is '是否启用';
comment on column "sys_tenant"."built_in" is '是否内置';
comment on column "sys_tenant"."create_user_id" is '创建者id';
comment on column "sys_tenant"."create_user_name" is '创建者名称';
comment on column "sys_tenant"."create_time" is '创建时间';
comment on column "sys_tenant"."update_user_id" is '更新者id';
comment on column "sys_tenant"."update_user_name" is '更新者名称';
comment on column "sys_tenant"."update_time" is '更新时间';
--endregion DDL


--region DML
merge into "sys_tenant" ("id", "name", "timezone", "default_language_code", "remark", "active", "built_in")
    values ('818772a0-c053-4634-a5e5-31c486b3146a', 'default-tenant', null, null, null, true, true);
--endregion DML