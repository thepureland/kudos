--region DDL
-- ip_start / ip_end：NUMERIC(39,0) 存无符号整数语义——ipv4 为 32 位起止；ipv6 为 128 位起止（单值一整段，不拆高低位）
create table if not exists "sys_access_rule_ip"
(
    "id"                character(36) default RANDOM_UUID() not null primary key,
    "ip_start"          numeric(39, 0)                      not null,
    "ip_end"            numeric(39, 0)                      not null,
    "ip_type_dict_code" character(4)               not null,
    "expiration_time"   timestamp(6),
    "parent_rule_id"    character(36)                        not null,
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

-- ipv4 / ipv6 均要求数值区间 start <= end；其它历史字典码不强制
alter table "sys_access_rule_ip"
    add constraint "chk_sys_access_rule_ip_range"
        check (
                (trim("ip_type_dict_code") in ('ipv4', 'ipv6') and "ip_start" <= "ip_end")
                or (trim("ip_type_dict_code") not in ('ipv4', 'ipv6'))
            );


comment on table "sys_access_rule_ip" is 'ip访问规则';
comment on column "sys_access_rule_ip"."id" is '主键';
comment on column "sys_access_rule_ip"."ip_start" is 'ip起（ipv4为32位无符号；ipv6为128位无符号，十进制存）';
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
