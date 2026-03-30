--region DDL

create table if not exists "sys_dict_item" (
    "id" character(36) default random_uuid() not null primary key,
    "item_code" character varying(64) not null,
    "item_name" character varying(64) not null,
    "dict_id" character varying(36) not null,
    "order_num" integer,
    "parent_id" character(36),
    "remark" character varying(300),
    "active" boolean default true not null,
    "built_in" boolean default false not null,
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6) default now(),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6),
    constraint "fk_sys_dict_item"
        foreign key ("dict_id") references "sys_dict" ("id")
);

create unique index if not exists "uq_sys_dict_item"
    on "sys_dict_item" ("item_code", "dict_id");

comment on table "sys_dict_item" is '字典项';
comment on column "sys_dict_item"."id" is '主键';
comment on column "sys_dict_item"."item_code" is '字典项代码';
comment on column "sys_dict_item"."item_name" is '字典项名称或其国际化key';
comment on column "sys_dict_item"."dict_id" is '字典ID';
comment on column "sys_dict_item"."order_num" is '字典项排序';
comment on column "sys_dict_item"."parent_id" is '父ID';
comment on column "sys_dict_item"."remark" is '备注';
comment on column "sys_dict_item"."active" is '是否启用';
comment on column "sys_dict_item"."built_in" is '是否内置';
comment on column "sys_dict_item"."create_user_id" is '创建者ID';
comment on column "sys_dict_item"."create_user_name" is '创建者名称';
comment on column "sys_dict_item"."create_time" is '创建时间';
comment on column "sys_dict_item"."update_user_id" is '更新者ID';
comment on column "sys_dict_item"."update_user_name" is '更新者名称';
comment on column "sys_dict_item"."update_time" is '更新时间';

--endregion DDL


--region DML

-- ds_use
insert into "sys_dict_item" ("dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('68139ed2-dbce-47fa-ac0d-2932fb0ee5ad', 'local', 'ds_use.local', 1, '本地数据源', true),
    ('68139ed2-dbce-47fa-ac0d-2932fb0ee5ad', 'remote', 'ds_use.remote', 1, '远程数据源', true),
    ('68139ed2-dbce-47fa-ac0d-2932fb0ee5ad', 'report', 'ds_use.report', 1, '报表数据源', true),
    ('68139ed2-dbce-47fa-ac0d-2932fb0ee5ad', 'readonly', 'ds_use.readonly', 1, '只读数据源', true);

-- ds_type
insert into "sys_dict_item" ("dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('d9f17338-8751-4d3b-bdd1-91a1b6f42432', 'hikariCP', 'ds_type.hikariCP', 1, 'hikariCP', true);

-- resource_type
insert into "sys_dict_item" ("dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('339b4cf1-6af4-49db-be1c-ee606959a689', '2', 'resource_type.2', 2, '功能', true),
    ('339b4cf1-6af4-49db-be1c-ee606959a689', '1', 'resource_type.1', 1, '菜单', true);

-- cache_strategy
insert into "sys_dict_item" ("dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('2601c57f-3900-4be8-9ebf-e79781db9d3d', 'SINGLE_LOCAL', 'cache_strategy.SINGLE_LOCAL', 1, '单节点本地缓存', true),
    ('2601c57f-3900-4be8-9ebf-e79781db9d3d', 'REMOTE', 'cache_strategy.REMOTE', 1, '远程缓存', true),
    ('2601c57f-3900-4be8-9ebf-e79781db9d3d', 'LOCAL_REMOTE', 'cache_strategy.LOCAL_REMOTE', 1, '本地-远程两级联动缓存', true);

-- i18n_type
insert into "sys_dict_item" ("dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('54094f46-dddb-41a2-b747-0eaa7d0ekil6', 'dict', 'i18n_type.dict', 1, '字典', true),
    ('54094f46-dddb-41a2-b747-0eaa7d0ekil6', 'dict-item', 'i18n_type.dict-item', 2, '字典项', true),
    ('54094f46-dddb-41a2-b747-0eaa7d0ekil6', 'valid-msg', 'i18n_type.valid-msg', 3, '验证提示', true),
    ('54094f46-dddb-41a2-b747-0eaa7d0ekil6', 'error-msg', 'i18n_type.error-msg', 4, '错误提示', true),
    ('54094f46-dddb-41a2-b747-0eaa7d0ekil6', 'view', 'i18n_type.view', 5, '页面', true);


-- terminal_type
    insert into "sys_dict_item" ("dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('ad52c551-01c1-4c7f-9a96-720eecb32885', '1', 'terminal_type.1', 2, 'PC端', true),
    ('ad52c551-01c1-4c7f-9a96-720eecb32885', '2', 'terminal_type.2', 3, '手机端', true),
    ('ad52c551-01c1-4c7f-9a96-720eecb32885', '4', 'terminal_type.4', 4, '手机端H5-Android', true),
    ('ad52c551-01c1-4c7f-9a96-720eecb32885', '8', 'terminal_type.8', 5, '手机端H5-iOS', true),
    ('ad52c551-01c1-4c7f-9a96-720eecb32885', '9', 'terminal_type.9', 6, '安卓收藏桌面', true),
    ('ad52c551-01c1-4c7f-9a96-720eecb32885', '10', 'terminal_type.10', 7, 'iOS收藏桌面', true),
    ('ad52c551-01c1-4c7f-9a96-720eecb32885', '12', 'terminal_type.12', 8, '手机端Android', true),
    ('ad52c551-01c1-4c7f-9a96-720eecb32885', '16', 'terminal_type.16', 9, '手机端iOS', true);

-- locale
insert into "sys_dict_item" ("dict_id", "item_code", "item_name", "order_num", "built_in") values
    ('54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'zh-CN', 'locale.zh_CN', 0, true),
    ('54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'zh-TW', 'locale.zh_TW', 1, true),
    ('54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'en-US', 'locale.en_US', 2, true),
    ('54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'ja-JP', 'locale.ja_JP', 3, true),
    ('54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'ko-KR', 'locale.ko_KR', 4, true),
    ('54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'ru-RU', 'locale.ru_RU', 5, true),
    ('54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'in-ID', 'locale.in_ID', 6, true),
    ('54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'ar-AE', 'locale.ar_AE', 7, true),
    ('54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'fr-FR', 'locale.fr_FR', 8, true),
    ('54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'es-ES', 'locale.es_ES', 9, true),
    ('54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'pt-BR', 'locale.pt_BR', 10, true);

-- access_rule_type
insert into "sys_dict_item" ("dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('ad52c541-02c1-3c7f-1a96-a20eecb32881', '0', 'access_rule_type.0', 1, '不限制', true),
    ('ad52c541-02c1-3c7f-1a96-a20eecb32881', '1', 'access_rule_type.1', 2, '白名单', true),
    ('ad52c541-02c1-3c7f-1a96-a20eecb32881', '2', 'access_rule_type.2', 3, '黑名单', true),
    ('ad52c541-02c1-3c7f-1a96-a20eecb32881', '3', 'access_rule_type.3', 4, '白名单+黑名单', true);

--endregion DML
