--region DDL
create table if not exists "sys_atomic_service"
(
    "code"        character varying(32)  not null primary key,
    "name"        character varying(128) not null
        constraint "uq_sys_atomic_service" unique,
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

create unique index if not exists "uq_sys_atomic_service" on "sys_atomic_service" ("name");

comment on table "sys_atomic_service" is '原子服务';
comment on column "sys_atomic_service"."code" is '编码';
comment on column "sys_atomic_service"."name" is '名称';
comment on column "sys_atomic_service"."remark" is '备注';
comment on column "sys_atomic_service"."active" is '是否启用';
comment on column "sys_atomic_service"."built_in" is '是否内置';
comment on column "sys_atomic_service"."create_user_id" is '创建者id';
comment on column "sys_atomic_service"."create_user_name" is '创建者名称';
comment on column "sys_atomic_service"."create_time" is '创建时间';
comment on column "sys_atomic_service"."update_user_id" is '更新者id';
comment on column "sys_atomic_service"."update_user_name" is '更新者名称';
comment on column "sys_atomic_service"."update_time" is '更新时间';
--endregion DDL


--region DML
merge into "sys_atomic_service" ("code", "name", "remark", "active", "built_in")
    values ('kudos-sys', 'kudos-sys', null, true, true);
--endregion DML