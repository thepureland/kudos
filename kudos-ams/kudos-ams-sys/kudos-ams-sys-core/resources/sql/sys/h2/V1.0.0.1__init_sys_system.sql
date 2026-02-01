--region DDL
create table if not exists "sys_system"
(
    "code"        character varying(32)  not null primary key,
    "name"        character varying(128) not null
        constraint "uq_sys_system" unique,
    "remark"      character varying(256),
    "active"      boolean default TRUE   not null,
    "built_in"    boolean default FALSE,
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6)
);

comment on table "sys_system" is '系统';
comment on column "sys_system"."code" is '编码';
comment on column "sys_system"."name" is '名称';
comment on column "sys_system"."remark" is '备注';
comment on column "sys_system"."active" is '是否启用';
comment on column "sys_system"."built_in" is '是否内置';
comment on column "sys_system"."create_user_id" is '创建者id';
comment on column "sys_system"."create_user_name" is '创建者名称';
comment on column "sys_system"."create_time" is '创建时间';
comment on column "sys_system"."update_user_id" is '更新者id';
comment on column "sys_system"."update_user_name" is '更新者名称';
comment on column "sys_system"."update_time" is '更新时间';
--endregion DDL


--region DML
merge into "sys_system" ("code", "name", "remark", "active", "built_in")
    values ('default', 'default-system', null, true, true);
--endregion DML