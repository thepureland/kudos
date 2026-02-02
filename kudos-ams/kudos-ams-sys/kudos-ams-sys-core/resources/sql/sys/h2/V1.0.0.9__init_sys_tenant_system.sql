--region DDL
create table if not exists "sys_tenant_system"
(
    "id"              char(36) default RANDOM_UUID() not null primary key,
    "tenant_id"       character varying(36)          not null,
    "system_code"     character varying(32)          not null,
    "create_user_id"  character varying(36),
    "create_user_name" character varying(32),
    "create_time"     timestamp(6),
    "update_user_id"  character varying(36),
    "update_user_name" character varying(32),
    "update_time"     timestamp(6)
);

create unique index if not exists "uq_sys_tenant_system"
    on "sys_tenant_system" ("tenant_id", "system_code");

create index if not exists "idx_sys_tenant_system_tenant_id" on "sys_tenant_system" ("tenant_id");

-- alter table "sys_tenant_system"
--     add constraint "fk_sys_tenant_system_tenant"
--         foreign key ("tenant_id") references "sys_tenant" ("id");
--
-- alter table "sys_tenant_system"
--     add constraint "fk_sys_tenant_system_system"
--         foreign key ("system_code") references "sys_system" ("code");

comment on table "sys_tenant_system" is '租户-系统关系';
comment on column "sys_tenant_system"."id" is '主键';
comment on column "sys_tenant_system"."tenant_id" is '租户id';
comment on column "sys_tenant_system"."system_code" is '系统编码';
comment on column "sys_tenant_system"."create_user_id" is '创建者id';
comment on column "sys_tenant_system"."create_user_name" is '创建者名称';
comment on column "sys_tenant_system"."create_time" is '创建时间';
comment on column "sys_tenant_system"."update_user_id" is '更新者id';
comment on column "sys_tenant_system"."update_user_name" is '更新者名称';
comment on column "sys_tenant_system"."update_time" is '更新时间';
--endregion DDL


--region DML
merge into "sys_tenant_system" ("id", "tenant_id", "system_code")
    values ('a3846388-5e61-4b58-8fd8-3415a2782e56', '818772a0-c053-4634-a5e5-31c486b3146a', 'default');
--endregion DML
