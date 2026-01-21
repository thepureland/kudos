--region DDL
create table if not exists "sys_resource"
(
    "id"                      char(36)  default RANDOM_UUID() not null primary key,
    "name"                    character varying(64)           not null,
    "url"                     character varying(256),
    "resource_type_dict_code" char(1)                         not null,
    "parent_id"               char(36),
    "order_num"               int2,
    "icon"                    character varying(256),
    "sub_system_code"         character varying(32)           not null,
    "remark"                  character varying(256),
    "active"                  boolean   default TRUE          not null,
    "built_in"                boolean   default FALSE         not null,
    "create_user_id"          character varying(36),
    "create_user_name"        character varying(32),
    "create_time"             timestamp default now()         not null,
    "update_user_id"          character varying(36),
    "update_user_name"        character varying(32),
    "update_time"             timestamp
);

create unique index if not exists "uq_sys_resource" on "sys_resource" ("name", "sub_system_code");

comment on table "sys_resource" is '资源';
comment on column "sys_resource"."id" is '主键';
comment on column "sys_resource"."name" is '名称';
comment on column "sys_resource"."url" is 'url';
comment on column "sys_resource"."resource_type_dict_code" is '资源类型字典代码';
comment on column "sys_resource"."parent_id" is '父id';
comment on column "sys_resource"."order_num" is '在同父节点下的排序号';
comment on column "sys_resource"."icon" is '图标';
comment on column "sys_resource"."sub_system_code" is '子系统编码';
comment on column "sys_resource"."remark" is '备注';
comment on column "sys_resource"."active" is '是否启用';
comment on column "sys_resource"."built_in" is '是否内置';
comment on column "sys_resource"."create_user_id" is '创建者id';
comment on column "sys_resource"."create_user_name" is '创建者名称';
comment on column "sys_resource"."create_time" is '创建时间';
comment on column "sys_resource"."update_user_id" is '更新者id';
comment on column "sys_resource"."update_user_name" is '更新者名称';
comment on column "sys_resource"."update_time" is '更新时间';
--endregion DDL


--region DML

--endregion DML