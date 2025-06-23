--region DDL
create table if not exists "sys_tenant_language"
(
    "id"            character(36) default RANDOM_UUID() not null primary key,
    "tenant_id"     character varying(36)               not null,
    "language_code" character varying(32)               not null,
    "create_user"   character varying(36),
    "create_time"   timestamp(6)  default LOCALTIMESTAMP,
    "update_user"   character varying(36),
    "update_time"   timestamp(6)
);

create unique index if not exists "uq_sys_tenant_language"
    on "sys_tenant_language" ("tenant_id", "language_code");

comment on table "sys_tenant_language" is '租户-语言关系';
comment on column "sys_tenant_language"."id" is '主键';
comment on column "sys_tenant_language"."tenant_id" is '租户id';
comment on column "sys_tenant_language"."language_code" is '语言代码';
comment on column "sys_tenant_language"."create_user" is '创建用户';
comment on column "sys_tenant_language"."create_time" is '创建时间';
comment on column "sys_tenant_language"."update_user" is '更新用户';
comment on column "sys_tenant_language"."update_time" is '更新时间';
--endregion DDL