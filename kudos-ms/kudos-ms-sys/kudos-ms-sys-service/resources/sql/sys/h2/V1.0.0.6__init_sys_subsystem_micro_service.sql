--region DDL
create table if not exists "sys_sub_system_micro_service"
(
    "id"                 char(36) default RANDOM_UUID() not null primary key,
    "sub_system_code"    character varying(32)          not null,
    "micro_service_code" character varying(32)          not null,
    "create_user"        character varying(36),
    "create_time"        timestamp(6),
    "update_user"        character varying(36),
    "update_time"        timestamp(6)
);

create unique index if not exists "uq_sys_sub_system_micro_service"
    on "sys_sub_system_micro_service" ("sub_system_code", "micro_service_code");

comment on table "sys_sub_system_micro_service" is '子系统-微服务关系';
comment on column "sys_sub_system_micro_service"."sub_system_code" is '子系统编码';
comment on column "sys_sub_system_micro_service"."micro_service_code" is '微服务编码';
comment on column "sys_sub_system_micro_service"."create_user" is '创建用户';
comment on column "sys_sub_system_micro_service"."create_time" is '创建时间';
comment on column "sys_sub_system_micro_service"."update_user" is '更新用户';
comment on column "sys_sub_system_micro_service"."update_time" is '更新时间';
--endregion DDL


--region DML
merge into "sys_sub_system_micro_service" ("id", "sub_system_code", "micro_service_code")
    values ('a9638b2b-52a2-47e1-b88a-2138ca1b2274', 'default', 'default');
--endregion DML