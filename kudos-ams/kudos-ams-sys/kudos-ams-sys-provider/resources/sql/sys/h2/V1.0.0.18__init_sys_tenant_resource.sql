--region DDL
create table "sys_tenant_resource"
(
    "id"          character(36) default RANDOM_UUID() not null primary key,
    "tenant_id"   character varying(36),
    "resource_id" character(36)
);

create unique index if not exists "uq_sys_tenant_resource"
    on "sys_tenant_resource" ("tenant_id", "resource_id");

create index if not exists "idx_sys_tenant_resource_tenant_id" on "sys_tenant_resource" ("tenant_id");

-- alter table "sys_tenant_resource"
--     add constraint "fk_sys_tenant_resource_tenant"
--         foreign key ("tenant_id") references "sys_tenant" ("id");
--
-- alter table "sys_tenant_resource"
--     add constraint "fk_sys_tenant_resource_resource"
--         foreign key ("resource_id") references "sys_resource" ("id");

comment on table "sys_tenant_resource" is '租户-资源关系';
comment on column "sys_tenant_resource"."id" is '主键';
comment on column "sys_tenant_resource"."tenant_id" is '租户id';
comment on column "sys_tenant_resource"."resource_id" is '资源id';
--endregion DDL