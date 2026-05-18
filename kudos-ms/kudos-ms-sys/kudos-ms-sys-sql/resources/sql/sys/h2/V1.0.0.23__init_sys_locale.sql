--region DDL
create table if not exists "sys_locale"
(
    "id"               char(36)     default RANDOM_UUID() not null primary key,
    "code"             character varying(32)              not null,
    "display_name"     character varying(64)              not null,
    "english_name"     character varying(64)              not null,
    "sort_no"          int4         default 0             not null,
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

create unique index if not exists "uq_sys_locale_code" on "sys_locale" ("code");

comment on table "sys_locale" is '语言/区域代码字典';
comment on column "sys_locale"."id" is '主键';
comment on column "sys_locale"."code" is '语言代码(如 zh_CN, en_US)';
comment on column "sys_locale"."display_name" is '显示名称(母语写法，如 简体中文)';
comment on column "sys_locale"."english_name" is '英文名称(如 Simplified Chinese)';
comment on column "sys_locale"."sort_no" is '排序号';
comment on column "sys_locale"."remark" is '备注';
comment on column "sys_locale"."active" is '是否启用';
comment on column "sys_locale"."built_in" is '是否内置';
comment on column "sys_locale"."create_user_id" is '创建者id';
comment on column "sys_locale"."create_user_name" is '创建者名称';
comment on column "sys_locale"."create_time" is '创建时间';
comment on column "sys_locale"."update_user_id" is '更新者id';
comment on column "sys_locale"."update_user_name" is '更新者名称';
comment on column "sys_locale"."update_time" is '更新时间';

-- 内置常用语言
insert into "sys_locale" ("code", "display_name", "english_name", "sort_no", "built_in")
values ('zh_CN', '简体中文', 'Simplified Chinese', 10, TRUE);
insert into "sys_locale" ("code", "display_name", "english_name", "sort_no", "built_in")
values ('zh_TW', '繁體中文', 'Traditional Chinese', 20, TRUE);
insert into "sys_locale" ("code", "display_name", "english_name", "sort_no", "built_in")
values ('en_US', 'English', 'English', 30, TRUE);
--endregion DDL
