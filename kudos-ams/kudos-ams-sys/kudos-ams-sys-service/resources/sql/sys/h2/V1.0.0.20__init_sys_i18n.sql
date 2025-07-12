--region DDL
create table if not exists "sys_i18n"
(
    "id"                  character(36) default RANDOM_UUID() not null primary key,
    "locale"              character varying(8)                not null,
    "module_code"         character varying(32)               not null,
    "i18n_type_dict_code" character varying(32)               not null,
    "key"                 character varying(128)              not null,
    "value"               character varying(1000)             not null,
    "active"              boolean       default TRUE          not null,
    "built_in"            boolean       default FALSE         not null,
    "create_user"         character varying(36),
    "create_time"         timestamp     default now()         not null,
    "update_user"         character varying(36),
    "update_time"         timestamp
);

create unique index if not exists "uq_sys_i18n__locale_module_type_key"
    on "sys_i18n" ("locale", "module_code", "i18n_type_dict_code", "key");

comment on table "sys_i18n" is '国际化';
comment on column "sys_i18n"."id" is '主键';
comment on column "sys_i18n"."locale" is '语言_地区';
comment on column "sys_i18n"."module_code" is '语言_地区';
comment on column "sys_i18n"."i18n_type_dict_code" is '国际化类型字典代码';
comment on column "sys_i18n"."key" is '国际化key';
comment on column "sys_i18n"."value" is '国际化值';
comment on column "sys_i18n"."active" is '是否启用';
comment on column "sys_i18n"."built_in" is '是否内置';
comment on column "sys_i18n"."create_user" is '创建用户';
comment on column "sys_i18n"."create_time" is '创建时间';
comment on column "sys_i18n"."update_user" is '更新用户';
comment on column "sys_i18n"."update_time" is '更新时间';
--endregion DDL