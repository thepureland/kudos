--region DDL
create table if not exists "sys_access_rule"
(
    "id"                  character(36) default RANDOM_UUID() not null primary key,
    "tenant_id"           character varying(36),
    "sub_system_code"     character varying(32),
    "system_code"         character varying(32)               not null,
    "rule_type_dict_code" character varying(32)               not null,
    "remark"              character varying(128),
    "active"              boolean       default TRUE          not null,
    "built_in"            boolean       default FALSE         not null,
    "create_user_id"      character varying(36),
    "create_user_name"    character varying(32),
    "create_time"         timestamp(6),
    "update_user_id"      character varying(36),
    "update_user_name"    character varying(32),
    "update_time"         timestamp(6),
    constraint "uq_sys_access_rule"
        unique ("tenant_id", "sub_system_code", "system_code")
);

create index if not exists "idx_sys_access_rule_tenant_id" on "sys_access_rule" ("tenant_id");

-- alter table "sys_access_rule"
--     add constraint "fk_sys_access_rule_tenant"
--         foreign key ("tenant_id") references "sys_tenant" ("id");
--
-- alter table "sys_access_rule"
--     add constraint "fk_sys_access_rule_sub_system"
--         foreign key ("sub_system_code") references "sys_sub_system" ("code");
--
-- alter table "sys_access_rule"
--     add constraint "fk_sys_access_rule_system"
--         foreign key ("system_code") references "sys_system" ("code");

comment on table "sys_access_rule" is '访问规则';
comment on column "sys_access_rule"."id" is '主键';
comment on column "sys_access_rule"."tenant_id" is '租户id';
comment on column "sys_access_rule"."sub_system_code" is '子系统编码';
comment on column "sys_access_rule"."system_code" is '系统编码';
comment on column "sys_access_rule"."rule_type_dict_code" is '规则类型字典代码';
comment on column "sys_access_rule"."remark" is '备注';
comment on column "sys_access_rule"."active" is '是否启用';
comment on column "sys_access_rule"."built_in" is '是否内置';
comment on column "sys_access_rule"."create_user_id" is '创建者id';
comment on column "sys_access_rule"."create_user_name" is '创建者名称';
comment on column "sys_access_rule"."create_time" is '创建时间';
comment on column "sys_access_rule"."update_user_id" is '更新者id';
comment on column "sys_access_rule"."update_user_name" is '更新者名称';
comment on column "sys_access_rule"."update_time" is '更新时间';
--endregion DDL