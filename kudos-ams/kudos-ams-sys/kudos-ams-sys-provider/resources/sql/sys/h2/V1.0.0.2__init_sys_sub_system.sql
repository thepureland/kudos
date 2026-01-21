--region DDL
create table if not exists "sys_sub_system"
(
    "code"        character varying(32)  not null primary key,
    "name"        character varying(128) not null
        constraint "uq_sys_sub_system" unique,
    "portal_code" character varying(32)  not null,
    "remark"      character varying(256),
    "active"      boolean default TRUE   not null,
    "built_in"    boolean default FALSE,
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6),
    constraint "fk_sys_sub_system"
        foreign key ("portal_code") references "sys_portal" ("code")
);

comment on table "sys_sub_system" is '子系统';
comment on column "sys_sub_system"."code" is '编码';
comment on column "sys_sub_system"."name" is '名称';
comment on column "sys_sub_system"."portal_code" is '门户编码';
comment on column "sys_sub_system"."remark" is '备注';
comment on column "sys_sub_system"."active" is '是否启用';
comment on column "sys_sub_system"."built_in" is '是否内置';
comment on column "sys_sub_system"."create_user_id" is '创建者id';
comment on column "sys_sub_system"."create_user_name" is '创建者名称';
comment on column "sys_sub_system"."create_time" is '创建时间';
comment on column "sys_sub_system"."update_user_id" is '更新者id';
comment on column "sys_sub_system"."update_user_name" is '更新者名称';
comment on column "sys_sub_system"."update_time" is '更新时间';
--endregion DDL


--region DML
merge into "sys_sub_system" ("code", "name", "portal_code", "remark", "active", "built_in")
    values ('default', 'default-sub_system', 'default', null, true, true);
--endregion DML