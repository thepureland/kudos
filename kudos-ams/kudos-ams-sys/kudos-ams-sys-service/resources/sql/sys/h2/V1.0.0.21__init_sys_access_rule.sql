--region DDL
create table if not exists "sys_access_rule"
(
    "id"              character(36) default RANDOM_UUID() not null primary key,
    "tenant_id"       character varying(36)               not null,
    "sub_system_code" character varying(32),
    "portal_code"     character varying(32)               not null,
    "rule_type"       smallint      default 1             not null,
    "create_user"     character varying(36),
    "create_time"     timestamp(6),
    "update_user"     character varying(36),
    "update_time"     timestamp(6),
    constraint "uq_sys_access_rule"
        unique ("tenant_id", "sub_system_code", "portal_code")
);

comment on table "sys_access_rule" is '访问规则';
comment on column "sys_access_rule"."id" is '主键';
comment on column "sys_access_rule"."tenant_id" is '租户id';
comment on column "sys_access_rule"."sub_system_code" is '子系统编码';
comment on column "sys_access_rule"."portal_code" is '门户编码';
comment on column "sys_access_rule"."rule_type" is '规则类型';
comment on column "sys_access_rule"."create_user" is '创建用户';
comment on column "sys_access_rule"."create_time" is '创建时间';
comment on column "sys_access_rule"."update_user" is '更新用户';
comment on column "sys_access_rule"."update_time" is '更新时间';
--endregion DDL