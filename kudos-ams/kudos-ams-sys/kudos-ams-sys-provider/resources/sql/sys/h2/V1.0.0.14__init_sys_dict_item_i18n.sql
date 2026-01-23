--region DDL
create table if not exists "sys_dict_item_i18n"
(
    "id"          char(36)  default RANDOM_UUID() not null primary key,
    "locale"      char(5)                         not null,
    "i18n_value"  character varying(1000)         not null,
    "item_id"     char(36)                        not null,
    "active"      boolean   default TRUE          not null,
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6) default now(),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6),
    constraint "fk_sys_dict_item_i18n"
        foreign key ("item_id") references "sys_dict_item" ("id")
);

create unique index if not exists "uq_sys_dict_item_i18n" on "sys_dict_item_i18n" ("locale", "item_id");

comment on table "sys_dict_item_i18n" is '字典项国际化';
comment on column "sys_dict_item_i18n"."id" is '主键';
comment on column "sys_dict_item_i18n"."locale" is '语言_地区';
comment on column "sys_dict_item_i18n"."i18n_value" is '国际化值';
comment on column "sys_dict_item_i18n"."item_id" is '字典项id';
comment on column "sys_dict_item_i18n"."active" is '是否启用';
comment on column "sys_dict_item_i18n"."create_user_id" is '创建者id';
comment on column "sys_dict_item_i18n"."create_user_name" is '创建者名称';
comment on column "sys_dict_item_i18n"."create_time" is '创建时间';
comment on column "sys_dict_item_i18n"."update_user_id" is '更新者id';
comment on column "sys_dict_item_i18n"."update_user_name" is '更新者名称';
comment on column "sys_dict_item_i18n"."update_time" is '更新时间';
--endregion DDL