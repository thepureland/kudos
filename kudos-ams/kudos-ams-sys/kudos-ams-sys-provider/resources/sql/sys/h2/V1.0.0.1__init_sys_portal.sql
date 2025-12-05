--region DDL
create table if not exists "sys_portal"
(
    "code"        character varying(32)  not null primary key,
    "name"        character varying(128) not null
        constraint "uq_sys_portal" unique,
    "remark"      character varying(256),
    "active"      boolean default TRUE   not null,
    "built_in"    boolean default FALSE,
    "create_user" character varying(36),
    "create_time" timestamp(6),
    "update_user" character varying(36),
    "update_time" timestamp(6)
);

comment on table "sys_portal" is '门户';
comment on column "sys_portal"."code" is '编码';
comment on column "sys_portal"."name" is '名称';
comment on column "sys_portal"."remark" is '备注';
comment on column "sys_portal"."active" is '是否启用';
comment on column "sys_portal"."built_in" is '是否内置';
comment on column "sys_portal"."create_user" is '创建用户';
comment on column "sys_portal"."create_time" is '创建时间';
comment on column "sys_portal"."update_user" is '更新用户';
comment on column "sys_portal"."update_time" is '更新时间';
--endregion DDL


--region DML
merge into "sys_portal" ("code", "name", "remark", "active", "built_in")
    values ('default', 'default-portal', null, true, true);
--endregion DML