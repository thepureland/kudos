--region DDL
create table if not exists "sys_param"
(
    "id"            character(36) default RANDOM_UUID() not null primary key,
    "param_name"    character varying(32)               not null,
    "param_value"   character varying(128)              not null,
    "default_value" character varying(128),
    "module_code"   character varying(32)               not null,
    "order_num"     int2,
    "remark"        character varying(128),
    "active"        boolean       default TRUE          not null,
    "built_in"      boolean       default FALSE         not null,
    "create_user"   character varying(36),
    "create_time"   timestamp     default now()         not null,
    "update_user"   character varying(36),
    "update_time"   timestamp
);

create unique index if not exists "uq_sys_param" on "sys_param" ("param_name", "module_code");

comment on table "sys_param" is '参数';
comment on column "sys_param"."id" is '主键';
comment on column "sys_param"."param_name" is '参数名称';
comment on column "sys_param"."param_value" is '参数值';
comment on column "sys_param"."default_value" is '默认参数值';
comment on column "sys_param"."module_code" is '模块';
comment on column "sys_param"."order_num" is '序号';
comment on column "sys_param"."remark" is '备注';
comment on column "sys_param"."active" is '是否启用';
comment on column "sys_param"."built_in" is '是否内置';
comment on column "sys_param"."create_user" is '创建用户';
comment on column "sys_param"."create_time" is '创建时间';
comment on column "sys_param"."update_user" is '更新用户';
comment on column "sys_param"."update_time" is '更新时间';
--endregion DDL

