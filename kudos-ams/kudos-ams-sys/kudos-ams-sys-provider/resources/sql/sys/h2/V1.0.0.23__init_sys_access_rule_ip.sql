--region DDL
create table if not exists "sys_access_rule_ip"
(
    "id"                character(36) default RANDOM_UUID() not null primary key,
    "ip_start"          bigint                              not null,
    "ip_end"            bigint                              not null,
    "ip_type_dict_code" character varying(36)               not null,
    "expiration_time"   timestamp(6),
    "parent_rule_id"    character varying(36)               not null,
    "remark"            character varying(128),
    "active"            boolean       default TRUE          not null,
    "built_in"          boolean       default FALSE         not null,
    "create_user_id"    character varying(36),
    "create_user_name"  character varying(32),
    "create_time"       timestamp(6),
    "update_user_id"    character varying(36),
    "update_user_name"  character varying(32),
    "update_time"       timestamp(6),
    constraint "fk_sys_access_rule_ip"
        foreign key ("parent_rule_id") references "sys_access_rule" ("id")
);


comment on table "sys_access_rule_ip" is 'ip访问规则';
comment on column "sys_access_rule_ip"."id" is '主键';
comment on column "sys_access_rule_ip"."ip_start" is 'ip起';
comment on column "sys_access_rule_ip"."ip_end" is 'ip止';
comment on column "sys_access_rule_ip"."ip_type_dict_code" is 'ip类型字典代码';
comment on column "sys_access_rule_ip"."expiration_time" is '过期时间';
comment on column "sys_access_rule_ip"."parent_rule_id" is '父规则id';
comment on column "sys_access_rule_ip"."remark" is '备注';
comment on column "sys_access_rule_ip"."active" is '是否启用';
comment on column "sys_access_rule_ip"."built_in" is '是否内置';
comment on column "sys_access_rule_ip"."create_user_id" is '创建者id';
comment on column "sys_access_rule_ip"."create_user_name" is '创建者名称';
comment on column "sys_access_rule_ip"."create_time" is '创建时间';
comment on column "sys_access_rule_ip"."update_user_id" is '更新者id';
comment on column "sys_access_rule_ip"."update_user_name" is '更新者名称';
comment on column "sys_access_rule_ip"."update_time" is '更新时间';
--endregion DDL