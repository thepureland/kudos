--region DDL
create table if not exists "sys_domain"
(
    "id"              character(36) default RANDOM_UUID() not null primary key,
    "domain"          character varying(64)               not null,
    "sub_system_code" character varying(32),
    "portal_code"     character varying(32)               not null,
    "tenant_id"       character varying(36),
    "remark"          character varying(128),
    "active"          boolean       default TRUE          not null,
    "built_in"        boolean       default FALSE         not null,
    "create_user"     character varying(36),
    "create_time"     timestamp     default now()         not null,
    "update_user"     character varying(36),
    "update_time"     timestamp
);

create unique index if not exists "uq_sys_domain" on "sys_domain" ("domain");

comment on table "sys_domain" is '域名';
comment on column "sys_domain"."id" is '主键';
comment on column "sys_domain"."domain" is '域名';
comment on column "sys_domain"."sub_system_code" is '子系统编码';
comment on column "sys_domain"."portal_code" is '门户编码';
comment on column "sys_domain"."tenant_id" is '租户id';
comment on column "sys_domain"."remark" is '备注';
comment on column "sys_domain"."active" is '是否启用';
comment on column "sys_domain"."built_in" is '是否内置';
comment on column "sys_domain"."create_user" is '创建用户';
comment on column "sys_domain"."create_time" is '创建时间';
comment on column "sys_domain"."update_user" is '更新用户';
comment on column "sys_domain"."update_time" is '更新时间';
--endregion DDL

