--region DDL
create table if not exists "auth_group"
(
    "id"                CHAR(36)  default RANDOM_UUID() not null primary key,
    "group_code"        VARCHAR(64)                     not null,
    "group_name"        VARCHAR(64)                     not null,
    "tenant_id"         varchar(36) not null,
    "subsys_code"       varchar(32) not null,
    "remark"            VARCHAR(128),
    "active"            BOOLEAN   default TRUE          not null,
    "built_in"          BOOLEAN   default FALSE         not null,
    "create_user_id"    character varying(36),
    "create_user_name"  character varying(32),
    "create_time"       timestamp(6),
    "update_user_id"    character varying(36),
    "update_user_name"  character varying(32),
    "update_time"       timestamp(6),
    constraint "uk_auth_group" unique ("group_code", "tenant_id")
    );

comment on table "auth_group" is '用户组';
comment on column "auth_group"."id" is '主键';
comment on column "auth_group"."group_code" is '用户编码';
comment on column "auth_group"."group_name" is '用户组名';
comment on column "auth_group"."tenant_id" is '租户id';
comment on column "auth_group"."subsys_code" is '子系统编码';
comment on column "auth_group"."remark" is '备注';
comment on column "auth_group"."active" is '是否激活';
comment on column "auth_group"."built_in" is '是否内置';
comment on column "auth_group"."create_user_id" is '创建者id';
comment on column "auth_group"."create_user_name" is '创建者名称';
comment on column "auth_group"."create_time" is '创建时间';
comment on column "auth_group"."update_user_id" is '更新者id';
comment on column "auth_group"."update_user_name" is '更新者名称';
comment on column "auth_group"."update_time" is '更新时间';

--endregion DDL


--region DML

--endregion DML