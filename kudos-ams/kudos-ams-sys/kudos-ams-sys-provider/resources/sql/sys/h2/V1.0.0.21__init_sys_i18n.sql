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
    "create_user_id"      character varying(36),
    "create_user_name"    character varying(32),
    "create_time"         timestamp(6)     default now()         not null,
    "update_user_id"      character varying(36),
    "update_user_name"    character varying(32),
    "update_time"         timestamp(6)
);

create unique index if not exists "uq_sys_i18n__locale_module_type_key"
    on "sys_i18n" ("locale", "module_code", "i18n_type_dict_code", "key");

-- alter table "sys_i18n"
--     add constraint "fk_sys_i18n_module"
--         foreign key ("module_code") references "sys_module" ("code");

comment on table "sys_i18n" is '国际化';
comment on column "sys_i18n"."id" is '主键';
comment on column "sys_i18n"."locale" is '语言_地区';
comment on column "sys_i18n"."module_code" is '语言_地区';
comment on column "sys_i18n"."i18n_type_dict_code" is '国际化类型字典代码';
comment on column "sys_i18n"."key" is '国际化key';
comment on column "sys_i18n"."value" is '国际化值';
comment on column "sys_i18n"."active" is '是否启用';
comment on column "sys_i18n"."built_in" is '是否内置';
comment on column "sys_i18n"."create_user_id" is '创建者id';
comment on column "sys_i18n"."create_user_name" is '创建者名称';
comment on column "sys_i18n"."create_time" is '创建时间';
comment on column "sys_i18n"."update_user_id" is '更新者id';
comment on column "sys_i18n"."update_user_name" is '更新者名称';
comment on column "sys_i18n"."update_time" is '更新时间';
--endregion DDL

--region DML

-- dict
merge into "sys_i18n" ("id", "locale", "module_code", "i18n_type_dict_code", "key", "value", "built_in")
    values
        ('26c199d9-b64e-i18n-dict-000000000001', 'zh_CN', 'sys', 'dict', 'dict.i18n_type', '国际化类型', true),
        ('26c199d9-b64e-i18n-dict-000000000002', 'zh_TW', 'sys', 'dict', 'dict.i18n_type', '國際化類型', true),
        ('26c199d9-b64e-i18n-dict-000000000003', 'en_US', 'sys', 'dict', 'dict.i18n_type', 'I18N Type', true);

-- dict-item
merge into "sys_i18n" ("id", "locale", "module_code", "i18n_type_dict_code", "key", "value", "built_in")
    values
    ('26c199d9-i18n-dict-item-000000000001', 'zh_CN', 'sys', 'dict-item', 'dict.item.zh_CN', '简体中文', true),
    ('26c199d9-i18n-dict-item-000000000002', 'zh_TW', 'sys', 'dict-item', 'dict.item.zh_CN', '簡體中文', true),
    ('26c199d9-i18n-dict-item-000000000003', 'en_US', 'sys', 'dict-item', 'dict.item.zh_CN', 'Simplified Chinese', true),

    ('26c199d9-i18n-dict-item-000000000004', 'zh_CN', 'sys', 'dict-item', 'dict.item.zh_TW', '繁体中文', true),
    ('26c199d9-i18n-dict-item-000000000005', 'zh_TW', 'sys', 'dict-item', 'dict.item.zh_TW', '繁體中文', true),
    ('26c199d9-i18n-dict-item-000000000006', 'en_US', 'sys', 'dict-item', 'dict.item.zh_TW', 'Traditional Chinese', true),

    ('26c199d9-i18n-dict-item-000000000007', 'zh_CN', 'sys', 'dict-item', 'dict.item.en_US', '英语（美国）', true),
    ('26c199d9-i18n-dict-item-000000000008', 'zh_TW', 'sys', 'dict-item', 'dict.item.en_US', '英語（美國）', true),
    ('26c199d9-i18n-dict-item-000000000009', 'en_US', 'sys', 'dict-item', 'dict.item.en_US', 'English (United States)', true),

    ('26c199d9-i18n-dict-item-000000000010', 'zh_CN', 'sys', 'dict-item', 'dict.item.ja_JP', '日语', true),
    ('26c199d9-i18n-dict-item-000000000011', 'zh_TW', 'sys', 'dict-item', 'dict.item.ja_JP', '日語', true),
    ('26c199d9-i18n-dict-item-000000000012', 'en_US', 'sys', 'dict-item', 'dict.item.ja_JP', 'Japanese', true),

    ('26c199d9-i18n-dict-item-000000000013', 'zh_CN', 'sys', 'dict-item', 'dict.item.ko_KR', '韩语', true),
    ('26c199d9-i18n-dict-item-000000000014', 'zh_TW', 'sys', 'dict-item', 'dict.item.ko_KR', '韓語', true),
    ('26c199d9-i18n-dict-item-000000000015', 'en_US', 'sys', 'dict-item', 'dict.item.ko_KR', 'Korean', true),

    ('26c199d9-i18n-dict-item-000000000016', 'zh_CN', 'sys', 'dict-item', 'dict.item.ru_RU', '俄语', true),
    ('26c199d9-i18n-dict-item-000000000017', 'zh_TW', 'sys', 'dict-item', 'dict.item.ru_RU', '俄語', true),
    ('26c199d9-i18n-dict-item-000000000018', 'en_US', 'sys', 'dict-item', 'dict.item.ru_RU', 'Russian', true),

    ('26c199d9-i18n-dict-item-000000000019', 'zh_CN', 'sys', 'dict-item', 'dict.item.in_ID', '印尼语', true),
    ('26c199d9-i18n-dict-item-000000000020', 'zh_TW', 'sys', 'dict-item', 'dict.item.in_ID', '印尼語', true),
    ('26c199d9-i18n-dict-item-000000000021', 'en_US', 'sys', 'dict-item', 'dict.item.in_ID', 'Indonesian', true),

    ('26c199d9-i18n-dict-item-000000000022', 'zh_CN', 'sys', 'dict-item', 'dict.item.ar_AE', '阿拉伯语（阿联酋）', true),
    ('26c199d9-i18n-dict-item-000000000023', 'zh_TW', 'sys', 'dict-item', 'dict.item.ar_AE', '阿拉伯語（阿聯酋）', true),
    ('26c199d9-i18n-dict-item-000000000024', 'en_US', 'sys', 'dict-item', 'dict.item.ar_AE', 'Arabic (United Arab Emirates)', true),

    ('26c199d9-i18n-dict-item-000000000025', 'zh_CN', 'sys', 'dict-item', 'dict.item.fr_FR', '法语', true),
    ('26c199d9-i18n-dict-item-000000000026', 'zh_TW', 'sys', 'dict-item', 'dict.item.fr_FR', '法語', true),
    ('26c199d9-i18n-dict-item-000000000027', 'en_US', 'sys', 'dict-item', 'dict.item.fr_FR', 'French', true),

    ('26c199d9-i18n-dict-item-000000000028', 'zh_CN', 'sys', 'dict-item', 'dict.item.es_ES', '西班牙语', true),
    ('26c199d9-i18n-dict-item-000000000029', 'zh_TW', 'sys', 'dict-item', 'dict.item.es_ES', '西班牙語', true),
    ('26c199d9-i18n-dict-item-000000000030', 'en_US', 'sys', 'dict-item', 'dict.item.es_ES', 'Spanish', true),

    ('26c199d9-i18n-dict-item-000000000031', 'zh_CN', 'sys', 'dict-item', 'dict.item.pt_BR', '葡萄牙语（巴西）', true),
    ('26c199d9-i18n-dict-item-000000000032', 'zh_TW', 'sys', 'dict-item', 'dict.item.pt_BR', '葡萄牙語（巴西）', true),
    ('26c199d9-i18n-dict-item-000000000033', 'en_US', 'sys', 'dict-item', 'dict.item.pt_BR', 'Portuguese (Brazil)', true);

--endregion DML