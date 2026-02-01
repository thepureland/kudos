--region DDL
create table if not exists "sys_i18n"
(
    "id"                  character(36) default RANDOM_UUID() not null primary key,
    "locale"              character varying(5)                not null,
    "atomic_service_code" character varying(32)               not null,
    "i18n_type_dict_code" character varying(32)               not null,
    "key"                 character varying(128)              not null,
    "value"               character varying(1000)             not null,
    "active"              boolean       default TRUE          not null,
    "built_in"            boolean       default FALSE         not null,
    "create_user_id"      character varying(36),
    "create_user_name"    character varying(32),
    "create_time"         timestamp(6) default now()          not null,
    "update_user_id"      character varying(36),
    "update_user_name"    character varying(32),
    "update_time"         timestamp(6)
);

create unique index if not exists "uq_sys_i18n"
    on "sys_i18n" ("locale", "atomic_service_code", "i18n_type_dict_code", "key");

-- alter table "sys_i18n"
--     add constraint "fk_sys_i18n_atomic_service"
--         foreign key ("atomic_service_code") references "sys_atomic_service" ("code");

comment on table "sys_i18n" is '国际化';
comment on column "sys_i18n"."id" is '主键';
comment on column "sys_i18n"."locale" is '语言_地区';
comment on column "sys_i18n"."atomic_service_code" is '原子服务编码';
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
merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('26c199d9-b64e-i18n-dict-000000000001', 'zh_CN', 'sys', 'dict', 'i18n_type', '国际化类型', true),
    ('26c199d9-b64e-i18n-dict-000000000002', 'zh_TW', 'sys', 'dict', 'i18n_type', '國際化類型', true),
    ('26c199d9-b64e-i18n-dict-000000000003', 'en_US', 'sys', 'dict', 'i18n_type', 'I18N Type', true);


-- dict-item ds_use
merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('26c199d9-i18n-dict-item-000000000001', 'zh_CN', 'sys', 'dict-item', 'ds_use.local', '本地数据源', true),
    ('26c199d9-i18n-dict-item-000000000002', 'zh_TW', 'sys', 'dict-item', 'ds_use.local', '本地資料來源', true),
    ('26c199d9-i18n-dict-item-000000000003', 'en_US', 'sys', 'dict-item', 'ds_use.local', 'Local Data Source', true),
    ('26c199d9-i18n-dict-item-000000000004', 'zh_CN', 'sys', 'dict-item', 'ds_use.remote', '远程数据源', true),
    ('26c199d9-i18n-dict-item-000000000005', 'zh_TW', 'sys', 'dict-item', 'ds_use.remote', '遠端資料來源', true),
    ('26c199d9-i18n-dict-item-000000000006', 'en_US', 'sys', 'dict-item', 'ds_use.remote', 'Remote Data Source', true),
    ('26c199d9-i18n-dict-item-000000000007', 'zh_CN', 'sys', 'dict-item', 'ds_use.report', '报表数据源', true),
    ('26c199d9-i18n-dict-item-000000000008', 'zh_TW', 'sys', 'dict-item', 'ds_use.report', '報表資料來源', true),
    ('26c199d9-i18n-dict-item-000000000009', 'en_US', 'sys', 'dict-item', 'ds_use.report', 'Report Data Source', true),
    ('26c199d9-i18n-dict-item-000000000010', 'zh_CN', 'sys', 'dict-item', 'ds_use.readonly', '只读数据源', true),
    ('26c199d9-i18n-dict-item-000000000011', 'zh_TW', 'sys', 'dict-item', 'ds_use.readonly', '唯讀資料來源', true),
    ('26c199d9-i18n-dict-item-000000000012', 'en_US', 'sys', 'dict-item', 'ds_use.readonly', 'Read-only Data Source', true);

-- dict-item ds_type
merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('26c199d9-i18n-dict-item-000000000013', 'zh_CN', 'sys', 'dict-item', 'ds_type.hikariCP', 'hikariCP', true),
    ('26c199d9-i18n-dict-item-000000000014', 'zh_TW', 'sys', 'dict-item', 'ds_type.hikariCP', 'hikariCP', true),
    ('26c199d9-i18n-dict-item-000000000015', 'en_US', 'sys', 'dict-item', 'ds_type.hikariCP', 'hikariCP', true);

-- dict-item resource_type
merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('26c199d9-i18n-dict-item-000000000016', 'zh_CN', 'sys', 'dict-item', 'resource_type.2', '功能', true),
    ('26c199d9-i18n-dict-item-000000000017', 'zh_TW', 'sys', 'dict-item', 'resource_type.2', '功能', true),
    ('26c199d9-i18n-dict-item-000000000018', 'en_US', 'sys', 'dict-item', 'resource_type.2', 'Function', true),
    ('26c199d9-i18n-dict-item-000000000019', 'zh_CN', 'sys', 'dict-item', 'resource_type.1', '菜单', true),
    ('26c199d9-i18n-dict-item-000000000020', 'zh_TW', 'sys', 'dict-item', 'resource_type.1', '選單', true),
    ('26c199d9-i18n-dict-item-000000000021', 'en_US', 'sys', 'dict-item', 'resource_type.1', 'Menu', true);

-- dict-item cache_strategy
merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('26c199d9-i18n-dict-item-000000000022', 'zh_CN', 'sys', 'dict-item', 'cache_strategy.SINGLE_LOCAL', '单节点本地缓存', true),
    ('26c199d9-i18n-dict-item-000000000023', 'zh_TW', 'sys', 'dict-item', 'cache_strategy.SINGLE_LOCAL', '單節點本地快取', true),
    ('26c199d9-i18n-dict-item-000000000024', 'en_US', 'sys', 'dict-item', 'cache_strategy.SINGLE_LOCAL', 'Single-node Local Cache', true),
    ('26c199d9-i18n-dict-item-000000000025', 'zh_CN', 'sys', 'dict-item', 'cache_strategy.REMOTE', '远程缓存', true),
    ('26c199d9-i18n-dict-item-000000000026', 'zh_TW', 'sys', 'dict-item', 'cache_strategy.REMOTE', '遠端快取', true),
    ('26c199d9-i18n-dict-item-000000000027', 'en_US', 'sys', 'dict-item', 'cache_strategy.REMOTE', 'Remote Cache', true),
    ('26c199d9-i18n-dict-item-000000000028', 'zh_CN', 'sys', 'dict-item', 'cache_strategy.LOCAL_REMOTE', '本地-远程两级联动缓存', true),
    ('26c199d9-i18n-dict-item-000000000029', 'zh_TW', 'sys', 'dict-item', 'cache_strategy.LOCAL_REMOTE', '本地-遠端兩級聯動快取', true),
    ('26c199d9-i18n-dict-item-000000000030', 'en_US', 'sys', 'dict-item', 'cache_strategy.LOCAL_REMOTE', 'Local-Remote Two-level Cache', true);

-- dict-item i18n_type
merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('26c199d9-i18n-dict-item-000000000031', 'zh_CN', 'sys', 'dict-item', 'i18n_type.dict', '字典', true),
    ('26c199d9-i18n-dict-item-000000000032', 'zh_TW', 'sys', 'dict-item', 'i18n_type.dict', '字典', true),
    ('26c199d9-i18n-dict-item-000000000033', 'en_US', 'sys', 'dict-item', 'i18n_type.dict', 'Dictionary', true),
    ('26c199d9-i18n-dict-item-000000000034', 'zh_CN', 'sys', 'dict-item', 'i18n_type.dict-item', '字典项', true),
    ('26c199d9-i18n-dict-item-000000000035', 'zh_TW', 'sys', 'dict-item', 'i18n_type.dict-item', '字典項', true),
    ('26c199d9-i18n-dict-item-000000000036', 'en_US', 'sys', 'dict-item', 'i18n_type.dict-item', 'Dictionary Item', true),
    ('26c199d9-i18n-dict-item-000000000037', 'zh_CN', 'sys', 'dict-item', 'i18n_type.view', '页面', true),
    ('26c199d9-i18n-dict-item-000000000038', 'zh_TW', 'sys', 'dict-item', 'i18n_type.view', '頁面', true),
    ('26c199d9-i18n-dict-item-000000000039', 'en_US', 'sys', 'dict-item', 'i18n_type.view', 'Page', true);

-- dict-item terminal_type
merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('26c199d9-i18n-dict-item-000000000040', 'zh_CN', 'sys', 'dict-item', 'terminal_type.1', 'PC端', true),
    ('26c199d9-i18n-dict-item-000000000041', 'zh_TW', 'sys', 'dict-item', 'terminal_type.1', 'PC端', true),
    ('26c199d9-i18n-dict-item-000000000042', 'en_US', 'sys', 'dict-item', 'terminal_type.1', 'PC', true),

    ('26c199d9-i18n-dict-item-000000000043', 'zh_CN', 'sys', 'dict-item', 'terminal_type.2', '手机端', true),
    ('26c199d9-i18n-dict-item-000000000044', 'zh_TW', 'sys', 'dict-item', 'terminal_type.2', '手機端', true),
    ('26c199d9-i18n-dict-item-000000000045', 'en_US', 'sys', 'dict-item', 'terminal_type.2', 'Mobile', true),

    ('26c199d9-i18n-dict-item-000000000046', 'zh_CN', 'sys', 'dict-item', 'terminal_type.4', '手机端H5-Android', true),
    ('26c199d9-i18n-dict-item-000000000047', 'zh_TW', 'sys', 'dict-item', 'terminal_type.4', '手機端H5-Android', true),
    ('26c199d9-i18n-dict-item-000000000048', 'en_US', 'sys', 'dict-item', 'terminal_type.4', 'Mobile H5-Android', true),

    ('26c199d9-i18n-dict-item-000000000049', 'zh_CN', 'sys', 'dict-item', 'terminal_type.8', '手机端H5-iOS', true),
    ('26c199d9-i18n-dict-item-000000000050', 'zh_TW', 'sys', 'dict-item', 'terminal_type.8', '手機端H5-iOS', true),
    ('26c199d9-i18n-dict-item-000000000051', 'en_US', 'sys', 'dict-item', 'terminal_type.8', 'Mobile H5-iOS', true),

    ('26c199d9-i18n-dict-item-000000000052', 'zh_CN', 'sys', 'dict-item', 'terminal_type.9', '安卓收藏桌面', true),
    ('26c199d9-i18n-dict-item-000000000053', 'zh_TW', 'sys', 'dict-item', 'terminal_type.9', 'Android桌面捷徑', true),
    ('26c199d9-i18n-dict-item-000000000054', 'en_US', 'sys', 'dict-item', 'terminal_type.9', 'Android Home Shortcut', true),

    ('26c199d9-i18n-dict-item-000000000055', 'zh_CN', 'sys', 'dict-item', 'terminal_type.10', 'iOS收藏桌面', true),
    ('26c199d9-i18n-dict-item-000000000056', 'zh_TW', 'sys', 'dict-item', 'terminal_type.10', 'iOS桌面捷徑', true),
    ('26c199d9-i18n-dict-item-000000000057', 'en_US', 'sys', 'dict-item', 'terminal_type.10', 'iOS Home Shortcut', true),

    ('26c199d9-i18n-dict-item-000000000058', 'zh_CN', 'sys', 'dict-item', 'terminal_type.12', '手机端Android', true),
    ('26c199d9-i18n-dict-item-000000000059', 'zh_TW', 'sys', 'dict-item', 'terminal_type.12', '手機端Android', true),
    ('26c199d9-i18n-dict-item-000000000060', 'en_US', 'sys', 'dict-item', 'terminal_type.12', 'Mobile Android', true),

    ('26c199d9-i18n-dict-item-000000000061', 'zh_CN', 'sys', 'dict-item', 'terminal_type.16', '手机端iOS', true),
    ('26c199d9-i18n-dict-item-000000000062', 'zh_TW', 'sys', 'dict-item', 'terminal_type.16', '手機端iOS', true),
    ('26c199d9-i18n-dict-item-000000000063', 'en_US', 'sys', 'dict-item', 'terminal_type.16', 'Mobile iOS', true);

-- dict-item access_rule_type
merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('26c199d9-i18n-dict-item-000000000064', 'zh_CN', 'sys', 'dict-item', 'access_rule_type.0', '不限制', true),
    ('26c199d9-i18n-dict-item-000000000065', 'zh_TW', 'sys', 'dict-item', 'access_rule_type.0', '不限制', true),
    ('26c199d9-i18n-dict-item-000000000066', 'en_US', 'sys', 'dict-item', 'access_rule_type.0', 'No Restriction', true),

    ('26c199d9-i18n-dict-item-000000000067', 'zh_CN', 'sys', 'dict-item', 'access_rule_type.1', '白名单', true),
    ('26c199d9-i18n-dict-item-000000000068', 'zh_TW', 'sys', 'dict-item', 'access_rule_type.1', '白名單', true),
    ('26c199d9-i18n-dict-item-000000000069', 'en_US', 'sys', 'dict-item', 'access_rule_type.1', 'Allowlist', true),

    ('26c199d9-i18n-dict-item-000000000070', 'zh_CN', 'sys', 'dict-item', 'access_rule_type.2', '黑名单', true),
    ('26c199d9-i18n-dict-item-000000000071', 'zh_TW', 'sys', 'dict-item', 'access_rule_type.2', '黑名單', true),
    ('26c199d9-i18n-dict-item-000000000072', 'en_US', 'sys', 'dict-item', 'access_rule_type.2', 'Blocklist', true),

    ('26c199d9-i18n-dict-item-000000000073', 'zh_CN', 'sys', 'dict-item', 'access_rule_type.3', '白名单+黑名单', true),
    ('26c199d9-i18n-dict-item-000000000074', 'zh_TW', 'sys', 'dict-item', 'access_rule_type.3', '白名單+黑名單', true),
    ('26c199d9-i18n-dict-item-000000000075', 'en_US', 'sys', 'dict-item', 'access_rule_type.3', 'Allowlist + Blocklist', true);

-- dict-item locale
merge into "sys_i18n" ("id", "locale", "atomic_service_code", "i18n_type_dict_code", "key", "value", "built_in") values
    ('26c199d9-i18n-dict-item-100000000001', 'zh_CN', 'sys', 'dict-item', 'dict.item.zh_CN', '简体中文', true),
    ('26c199d9-i18n-dict-item-100000000002', 'zh_TW', 'sys', 'dict-item', 'dict.item.zh_CN', '簡體中文', true),
    ('26c199d9-i18n-dict-item-100000000003', 'en_US', 'sys', 'dict-item', 'dict.item.zh_CN', 'Simplified Chinese', true),

    ('26c199d9-i18n-dict-item-100000000004', 'zh_CN', 'sys', 'dict-item', 'dict.item.zh_TW', '繁体中文', true),
    ('26c199d9-i18n-dict-item-100000000005', 'zh_TW', 'sys', 'dict-item', 'dict.item.zh_TW', '繁體中文', true),
    ('26c199d9-i18n-dict-item-100000000006', 'en_US', 'sys', 'dict-item', 'dict.item.zh_TW', 'Traditional Chinese', true),

    ('26c199d9-i18n-dict-item-100000000007', 'zh_CN', 'sys', 'dict-item', 'dict.item.en_US', '英语（美国）', true),
    ('26c199d9-i18n-dict-item-100000000008', 'zh_TW', 'sys', 'dict-item', 'dict.item.en_US', '英語（美國）', true),
    ('26c199d9-i18n-dict-item-100000000009', 'en_US', 'sys', 'dict-item', 'dict.item.en_US', 'English (United States)', true),

    ('26c199d9-i18n-dict-item-100000000010', 'zh_CN', 'sys', 'dict-item', 'dict.item.ja_JP', '日语', true),
    ('26c199d9-i18n-dict-item-100000000011', 'zh_TW', 'sys', 'dict-item', 'dict.item.ja_JP', '日語', true),
    ('26c199d9-i18n-dict-item-100000000012', 'en_US', 'sys', 'dict-item', 'dict.item.ja_JP', 'Japanese', true),

    ('26c199d9-i18n-dict-item-100000000013', 'zh_CN', 'sys', 'dict-item', 'dict.item.ko_KR', '韩语', true),
    ('26c199d9-i18n-dict-item-100000000014', 'zh_TW', 'sys', 'dict-item', 'dict.item.ko_KR', '韓語', true),
    ('26c199d9-i18n-dict-item-100000000015', 'en_US', 'sys', 'dict-item', 'dict.item.ko_KR', 'Korean', true),

    ('26c199d9-i18n-dict-item-100000000016', 'zh_CN', 'sys', 'dict-item', 'dict.item.ru_RU', '俄语', true),
    ('26c199d9-i18n-dict-item-100000000017', 'zh_TW', 'sys', 'dict-item', 'dict.item.ru_RU', '俄語', true),
    ('26c199d9-i18n-dict-item-100000000018', 'en_US', 'sys', 'dict-item', 'dict.item.ru_RU', 'Russian', true),

    ('26c199d9-i18n-dict-item-100000000019', 'zh_CN', 'sys', 'dict-item', 'dict.item.in_ID', '印尼语', true),
    ('26c199d9-i18n-dict-item-100000000020', 'zh_TW', 'sys', 'dict-item', 'dict.item.in_ID', '印尼語', true),
    ('26c199d9-i18n-dict-item-100000000021', 'en_US', 'sys', 'dict-item', 'dict.item.in_ID', 'Indonesian', true),

    ('26c199d9-i18n-dict-item-100000000022', 'zh_CN', 'sys', 'dict-item', 'dict.item.ar_AE', '阿拉伯语（阿联酋）', true),
    ('26c199d9-i18n-dict-item-100000000023', 'zh_TW', 'sys', 'dict-item', 'dict.item.ar_AE', '阿拉伯語（阿聯酋）', true),
    ('26c199d9-i18n-dict-item-100000000024', 'en_US', 'sys', 'dict-item', 'dict.item.ar_AE', 'Arabic (United Arab Emirates)', true),

    ('26c199d9-i18n-dict-item-100000000025', 'zh_CN', 'sys', 'dict-item', 'dict.item.fr_FR', '法语', true),
    ('26c199d9-i18n-dict-item-100000000026', 'zh_TW', 'sys', 'dict-item', 'dict.item.fr_FR', '法語', true),
    ('26c199d9-i18n-dict-item-100000000027', 'en_US', 'sys', 'dict-item', 'dict.item.fr_FR', 'French', true),

    ('26c199d9-i18n-dict-item-100000000028', 'zh_CN', 'sys', 'dict-item', 'dict.item.es_ES', '西班牙语', true),
    ('26c199d9-i18n-dict-item-100000000029', 'zh_TW', 'sys', 'dict-item', 'dict.item.es_ES', '西班牙語', true),
    ('26c199d9-i18n-dict-item-100000000030', 'en_US', 'sys', 'dict-item', 'dict.item.es_ES', 'Spanish', true),

    ('26c199d9-i18n-dict-item-100000000031', 'zh_CN', 'sys', 'dict-item', 'dict.item.pt_BR', '葡萄牙语（巴西）', true),
    ('26c199d9-i18n-dict-item-100000000032', 'zh_TW', 'sys', 'dict-item', 'dict.item.pt_BR', '葡萄牙語（巴西）', true),
    ('26c199d9-i18n-dict-item-100000000033', 'en_US', 'sys', 'dict-item', 'dict.item.pt_BR', 'Portuguese (Brazil)', true);


--endregion DML
