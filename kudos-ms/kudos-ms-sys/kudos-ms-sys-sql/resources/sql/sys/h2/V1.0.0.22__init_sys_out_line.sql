--region DDL
create table if not exists "sys_out_line"
(
    "id"               char(36)     default RANDOM_UUID() not null primary key,
    "name"             character varying(64)              not null,
    "host"             character varying(256)             not null,
    "port"             int4,
    "protocol"         character varying(16)              not null,
    "system_code"      character varying(32)              not null,
    "tenant_id"        char(36),
    "remark"           character varying(128),
    "active"           boolean      default TRUE          not null,
    "built_in"         boolean      default FALSE         not null,
    "create_user_id"   character varying(36),
    "create_user_name" character varying(32),
    "create_time"      timestamp(6) default now()         not null,
    "update_user_id"   character varying(36),
    "update_user_name" character varying(32),
    "update_time"     timestamp(6)
);

create unique index if not exists "uq_sys_out_line"
    on "sys_out_line" ("system_code", "tenant_id", "host", "port", "protocol");

create index if not exists "idx_sys_out_line_system_code" on "sys_out_line" ("system_code");
create index if not exists "idx_sys_out_line_tenant_id" on "sys_out_line" ("tenant_id");

comment on table "sys_out_line" is '出网白名单';
comment on column "sys_out_line"."id" is '主键';
comment on column "sys_out_line"."name" is '名称';
comment on column "sys_out_line"."host" is '主机名或通配符(如 *.example.com)';
comment on column "sys_out_line"."port" is '端口；NULL 表示任意端口';
comment on column "sys_out_line"."protocol" is '协议(http/https/tcp/any)';
comment on column "sys_out_line"."system_code" is '系统编码';
comment on column "sys_out_line"."tenant_id" is '租户id；NULL 表示平台级';
comment on column "sys_out_line"."remark" is '备注';
comment on column "sys_out_line"."active" is '是否启用';
comment on column "sys_out_line"."built_in" is '是否内置';
comment on column "sys_out_line"."create_user_id" is '创建者id';
comment on column "sys_out_line"."create_user_name" is '创建者名称';
comment on column "sys_out_line"."create_time" is '创建时间';
comment on column "sys_out_line"."update_user_id" is '更新者id';
comment on column "sys_out_line"."update_user_name" is '更新者名称';
comment on column "sys_out_line"."update_time" is '更新时间';
--endregion DDL
