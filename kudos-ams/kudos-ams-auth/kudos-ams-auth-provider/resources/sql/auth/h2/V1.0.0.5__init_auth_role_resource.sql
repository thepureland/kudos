--region DDL
create table if not exists "auth_role_resource"
(
    "id"          character(36) default RANDOM_UUID() not null primary key,
    "role_id"        character(36)  not null,
    "resource_id" character(36) not null,
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6),
    constraint "uk_auth_role_resource" unique ("role_id", "resource_id")
    );

comment on table "auth_role_resource" is '角色-资源';
comment on column "auth_role_resource"."id" is '主键';
comment on column "auth_role_resource"."role_id" is '角色id';
comment on column "auth_role_resource"."resource_id" is '资源id';
comment on column "auth_role_resource"."create_user_id" is '创建者id';
comment on column "auth_role_resource"."create_user_name" is '创建者名称';
comment on column "auth_role_resource"."create_time" is '创建时间';
comment on column "auth_role_resource"."update_user_id" is '更新者id';
comment on column "auth_role_resource"."update_user_name" is '更新者名称';
comment on column "auth_role_resource"."update_time" is '更新时间';

--endregion DDL


--region DML

--endregion DML
