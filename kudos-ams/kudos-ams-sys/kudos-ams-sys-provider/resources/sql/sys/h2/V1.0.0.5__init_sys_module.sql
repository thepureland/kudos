--region DDL
create table if not exists "sys_module"
(
    "code"                character varying(32)  not null primary key,
    "name"                character varying(128) not null,
    "atomic_service_code" character varying(32)  not null,
    "remark"              character varying(256),
    "active"              boolean default TRUE   not null,
    "built_in"            boolean default FALSE,
    "create_user_id"      character varying(36),
    "create_user_name"    character varying(32),
    "create_time"         timestamp(6),
    "update_user_id"      character varying(36),
    "update_user_name"    character varying(32),
    "update_time"         timestamp(6),
    constraint "fk_sys_module"
        foreign key ("atomic_service_code") references "sys_atomic_service" ("code")
);

create unique index if not exists "uq_sys_module" on "sys_module" ("name", "atomic_service_code");

comment on table "sys_module" is '模块';
comment on column "sys_module"."code" is '编码';
comment on column "sys_module"."name" is '名称';
comment on column "sys_module"."atomic_service_code" is '原子服务编码';
comment on column "sys_module"."remark" is '备注';
comment on column "sys_module"."active" is '是否启用';
comment on column "sys_module"."built_in" is '是否内置';
comment on column "sys_module"."create_user_id" is '创建者id';
comment on column "sys_module"."create_user_name" is '创建者名称';
comment on column "sys_module"."create_time" is '创建时间';
comment on column "sys_module"."update_user_id" is '更新者id';
comment on column "sys_module"."update_user_name" is '更新者名称';
comment on column "sys_module"."update_time" is '更新时间';
--endregion DDL


--region DML
merge into "sys_module" ("code", "name", "atomic_service_code", "remark", "active", "built_in")
    values ('kudos-sys', 'kudos-sys', 'kudos-sys', null, true, true);
--endregion DML