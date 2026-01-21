--region DDL
create table if not exists "sys_tenant_sub_system"
(
    "id"              char(36) default RANDOM_UUID() not null primary key,
    "tenant_id"       character varying(36)          not null,
    "sub_system_code" character varying(32)          not null,
    "portal_code"     character varying(32)          not null,
    "create_user_id"  character varying(36),
    "create_user_name" character varying(32),
    "create_time"     timestamp(6),
    "update_user_id"  character varying(36),
    "update_user_name" character varying(32),
    "update_time"     timestamp(6)
);

create unique index if not exists "uq_sys_tenant_sub_system"
    on "sys_tenant_sub_system" ("tenant_id", "sub_system_code", "portal_code");

comment on table "sys_tenant_sub_system" is '租户-子系统关系';
comment on column "sys_tenant_sub_system"."id" is '主键';
comment on column "sys_tenant_sub_system"."tenant_id" is '租户id';
comment on column "sys_tenant_sub_system"."sub_system_code" is '子系统编码';
comment on column "sys_tenant_sub_system"."portal_code" is '门户编码';
comment on column "sys_tenant_sub_system"."create_user_id" is '创建者id';
comment on column "sys_tenant_sub_system"."create_user_name" is '创建者名称';
comment on column "sys_tenant_sub_system"."create_time" is '创建时间';
comment on column "sys_tenant_sub_system"."update_user_id" is '更新者id';
comment on column "sys_tenant_sub_system"."update_user_name" is '更新者名称';
comment on column "sys_tenant_sub_system"."update_time" is '更新时间';
--endregion DDL


--region DML
merge into "sys_tenant_sub_system" ("id", "tenant_id", "sub_system_code", "portal_code")
    values ('a3846388-5e61-4b58-8fd8-3415a2782e56', '818772a0-c053-4634-a5e5-31c486b3146a', 'default', 'default');
--endregion DML