--region DDL
create table if not exists "sys_tenant_language"
(
    "id"            character(36) default RANDOM_UUID() not null primary key,
    "tenant_id"     character varying(36)               not null,
    "language_code" character varying(32)               not null,
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time"   timestamp(6)  default LOCALTIMESTAMP,
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time"   timestamp(6)
);

create unique index if not exists "uq_sys_tenant_language"
    on "sys_tenant_language" ("tenant_id", "language_code");

create index if not exists "idx_sys_tenant_language_tenant_id" on "sys_tenant_language" ("tenant_id");

-- alter table "sys_tenant_language"
--     add constraint "fk_sys_tenant_language_tenant"
--         foreign key ("tenant_id") references "sys_tenant" ("id");

comment on table "sys_tenant_language" is '租户-语言关系';
comment on column "sys_tenant_language"."id" is '主键';
comment on column "sys_tenant_language"."tenant_id" is '租户id';
comment on column "sys_tenant_language"."language_code" is '语言代码';
comment on column "sys_tenant_language"."create_user_id" is '创建者id';
comment on column "sys_tenant_language"."create_user_name" is '创建者名称';
comment on column "sys_tenant_language"."create_time" is '创建时间';
comment on column "sys_tenant_language"."update_user_id" is '更新者id';
comment on column "sys_tenant_language"."update_user_name" is '更新者名称';
comment on column "sys_tenant_language"."update_time" is '更新时间';
--endregion DDL