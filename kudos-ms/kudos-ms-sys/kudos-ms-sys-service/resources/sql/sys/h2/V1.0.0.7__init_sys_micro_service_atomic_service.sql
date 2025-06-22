--region DDL
create table if not exists "sys_micro_service_atomic_service"
(
    "id"                  char(36) default RANDOM_UUID() not null primary key,
    "micro_service_code"  character varying(32)          not null,
    "atomic_service_code" character varying(32)          not null,
    "create_user"         character varying(36),
    "create_time"         timestamp(6),
    "update_user"         character varying(36),
    "update_time"         timestamp(6)
);

create unique index if not exists "uq_sys_micro_service_atomic_service"
    on "sys_micro_service_atomic_service" ("micro_service_code", "atomic_service_code");

comment on table "sys_micro_service_atomic_service" is '微服务-原子服务关系';
comment on column "sys_micro_service_atomic_service"."micro_service_code" is '微服务编码';
comment on column "sys_micro_service_atomic_service"."atomic_service_code" is '原子服务编码';
comment on column "sys_micro_service_atomic_service"."create_user" is '创建用户';
comment on column "sys_micro_service_atomic_service"."create_time" is '创建时间';
comment on column "sys_micro_service_atomic_service"."update_user" is '更新用户';
comment on column "sys_micro_service_atomic_service"."update_time" is '更新时间';
--endregion DDL


--region DML
merge into "sys_micro_service_atomic_service" ("id", "micro_service_code", "atomic_service_code")
    values ('051c38f8-82d6-48b2-a8b0-c080b825234c', 'default', 'kudos-sys');
--endregion DML