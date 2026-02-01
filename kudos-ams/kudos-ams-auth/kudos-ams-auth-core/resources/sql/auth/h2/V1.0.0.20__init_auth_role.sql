--region DDL
create table if not exists "auth_role"
(
    "id"          character(36) default RANDOM_UUID() not null primary key,
    "code"        character varying(32)  not null,
    "name"        character varying(64)  not null,
    "tenant_id" varchar(36) not null,
    "subsys_code" varchar(32) not null,
    "remark"      character varying(256),
    "active"      boolean default TRUE   not null,
    "built_in"    boolean default FALSE,
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6),
    constraint "uk_auth_role" unique ("code", "tenant_id")
    );

comment on table "auth_role" is '角色';
comment on column "auth_role"."id" is '主键';
comment on column "auth_role"."code" is '角色编码';
comment on column "auth_role"."name" is '角色名称';
comment on column "auth_role"."tenant_id" is '租户id';
comment on column "auth_role"."subsys_code" is '子系统编码';
comment on column "auth_role"."remark" is '备注';
comment on column "auth_role"."active" is '是否激活';
comment on column "auth_role"."built_in" is '是否内置';
comment on column "auth_role"."create_user_id" is '创建者id';
comment on column "auth_role"."create_user_name" is '创建者名称';
comment on column "auth_role"."create_time" is '创建时间';
comment on column "auth_role"."update_user_id" is '更新者id';
comment on column "auth_role"."update_user_name" is '更新者名称';
comment on column "auth_role"."update_time" is '更新时间';

--endregion DDL


--region DML

--endregion DML