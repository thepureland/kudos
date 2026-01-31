--region DDL
create table if not exists "sys_dict_item"
(
    "id"
    character
(
    36
) DEFAULT RANDOM_UUID
(
) NOT NULL PRIMARY KEY,
    "item_code" character varying
(
    64
) NOT NULL,
    "item_name" character varying
(
    64
) NOT NULL,
    "dict_id" character varying
(
    36
) NOT NULL,
    "order_num" integer,
    "parent_id" character
(
    36
),
    "remark" character varying
(
    300
),
    "active" boolean DEFAULT TRUE NOT NULL,
    "built_in" boolean DEFAULT FALSE NOT NULL,
    "create_user_id" character varying
(
    36
),
    "create_user_name" character varying
(
    32
),
    "create_time" timestamp
(
    6
) default now
(
),
    "update_user_id" character varying
(
    36
),
    "update_user_name" character varying
(
    32
),
    "update_time" timestamp
(
    6
),
    constraint "fk_sys_dict_item"
    foreign key
(
    "dict_id"
) references "sys_dict"
(
    "id"
)
    );

create unique index if not exists "uq_sys_dict_item" on "sys_dict_item" ("item_code", "dict_id");

comment
on table "sys_dict_item" is '字典项';
comment
on column "sys_dict_item"."id" is '主键';
comment
on column "sys_dict_item"."item_code" is '字典项代码';
comment
on column "sys_dict_item"."item_name" is '字典项名称';
comment
on column "sys_dict_item"."dict_id" is '字典id';
comment
on column "sys_dict_item"."order_num" is '字典项排序';
comment
on column "sys_dict_item"."parent_id" is '父id';
comment
on column "sys_dict_item"."remark" is '备注';
comment
on column "sys_dict_item"."active" is '是否启用';
comment
on column "sys_dict_item"."built_in" is '是否内置';
comment
on column "sys_dict_item"."create_user_id" is '创建者id';
comment
on column "sys_dict_item"."create_user_name" is '创建者名称';
comment
on column "sys_dict_item"."create_time" is '创建时间';
comment
on column "sys_dict_item"."update_user_id" is '更新者id';
comment
on column "sys_dict_item"."update_user_name" is '更新者名称';
comment
on column "sys_dict_item"."update_time" is '更新时间';
--endregion DDL


--region DML

-- ds_use
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('e8ff3f9a-a57a-4183-953d-fe80c12fcd67', '68139ed2-dbce-47fa-ac0d-2932fb0ee5ad', 'local', 'ds_use.local', 1, '本地数据源', true),
    ('b9364152-1127-4129-9f65-101be811b33d', '68139ed2-dbce-47fa-ac0d-2932fb0ee5ad', 'remote', 'ds_use.remote', 1, '远程数据源', true),
    ('c46091d2-945c-4440-b103-ac58a7aeca09', '68139ed2-dbce-47fa-ac0d-2932fb0ee5ad', 'report', 'ds_use.report', 1, '报表数据源', true),
    ('b0e0bf5c-d2f8-46ee-9c1f-14049645d872', '68139ed2-dbce-47fa-ac0d-2932fb0ee5ad', 'readonly', 'ds_use.readonly', 1, '只读数据源', true);

-- ds_type
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('8aabaa7f-6d19-4d8a-8aed-a9f8ca55395e', 'd9f17338-8751-4d3b-bdd1-91a1b6f42432', 'hikariCP', 'ds_type.hikariCP', 1, 'hikariCP', true);

-- resource_type
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('d2e7c962-d0ca-43a5-b722-e1878dfa1575', '339b4cf1-6af4-49db-be1c-ee606959a689', '2', 'resource_type.2', 2, '功能', true),
    ('dd359907-6587-46d6-82a6-a5a3dd038ffc', '339b4cf1-6af4-49db-be1c-ee606959a689', '1', 'resource_type.1', 1, '菜单', true);

-- cache_strategy
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('09783dd9-9451-4f8c-8ca5-ae5999cc28cb', '2601c57f-3900-4be8-9ebf-e79781db9d3d', 'SINGLE_LOCAL', 'cache_strategy.SINGLE_LOCAL', 1, '单节点本地缓存', true),
    ('5fc9ecad-67ef-45a1-ac5f-ffa819d84e88', '2601c57f-3900-4be8-9ebf-e79781db9d3d', 'REMOTE', 'cache_strategy.REMOTE', 1, '远程缓存', true),
    ('9e0b76f7-68f7-4f02-81f1-36e9b1fa26bd', '2601c57f-3900-4be8-9ebf-e79781db9d3d', 'LOCAL_REMOTE', 'cache_strategy.LOCAL_REMOTE', 1, '本地-远程两级联动缓存', true);

-- locale
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "built_in") values
    ('26c199d9-b64e-4461-a445-59477b7a1395', '54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'zh_CN', 'locale.zh_CN', 0, true),
    ('dc5971ac-7c46-413c-99f0-95f256d74fb6', '54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'zh_TW', 'locale.zh_TW', 1, true),
    ('082150f2-c157-4ad5-ba11-98e3a73b35a2', '54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'en_US', 'locale.en_US', 2, true),
    ('5da2f572-b11e-4344-b054-86e2299251d5', '54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'ja_JP', 'locale.ja_JP', 3, true),
    ('1dbe8327-5401-4f2b-8976-1b6eb6ea168a', '54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'ko_KR', 'locale.ko_KR', 4, true),
    ('1dbe8327-5401-4f2b-8976-1b6eb6ea168a', '54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'ru_RU', 'locale.ru_RU', 5, true),
    ('89c5d589-7c92-4090-94c5-c092e8287866', '54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'in_ID', 'locale.in_ID', 6, true),
    ('89c5d589-7c92-4090-94c5-c092e8287866', '54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'ar_AE', 'locale.ar_AE', 7, true),
    ('89c5d589-7c92-4090-94c5-c092e8287866', '54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'fr_FR', 'locale.fr_FR', 8, true),
    ('b0c29b54-a292-4e7d-b5df-3b79beef7b15', '54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'es_ES', 'locale.es_ES', 9, true),
    ('a2b158b4-d42f-4dde-b020-67ae1486393a', '54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'pt_BR', 'locale.pt_BR', 10, true);

-- i18n_type
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('26c199d9-b64e-4461-i18n-type00000001', '54094f46-dddb-41a2-b747-0eaa7d0ekil6', 'dict', 'i18n_type.dict', 1, '字典', true),
    ('26c199d9-b64e-4461-i18n-type00000002', '54094f46-dddb-41a2-b747-0eaa7d0ekil6', 'dict-item', 'i18n_type.dict-item', 2, '字典项', true),
    ('26c199d9-b64e-4461-i18n-type00000003', '54094f46-dddb-41a2-b747-0eaa7d0ekil6', 'view', 'i18n_type.view', 3, '页面', true);

-- terminal_type
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('a2456908-e102-49a5-9fd0-82552e3560c9', 'ad52c551-01c1-4c7f-9a96-720eecb32885', '1', 'terminal_type.1', 2, 'PC端', true),
    ('d8f07398-8a95-4a55-9a49-299c955e03bc', 'ad52c551-01c1-4c7f-9a96-720eecb32885', '2', 'terminal_type.2', 3, '手机端', true),
    ('b4bafd4b-021e-402c-9f16-8249fa86cedd', 'ad52c551-01c1-4c7f-9a96-720eecb32885', '4', 'terminal_type.4', 4, '手机端H5-Android', true),
    ('f58aec33-4294-4431-8e6c-c43b6ca02ec3', 'ad52c551-01c1-4c7f-9a96-720eecb32885', '8', 'terminal_type.8', 5, '手机端H5-iOS', true),
    ('d36a0e12-310f-4f91-aecd-49adef85104d', 'ad52c551-01c1-4c7f-9a96-720eecb32885', '9', 'terminal_type.9', 6, '安卓收藏桌面', true),
    ('c323ddb9-ccba-4b99-bb00-172bcb654d05', 'ad52c551-01c1-4c7f-9a96-720eecb32885', '10', 'terminal_type.10', 7, 'iOS收藏桌面', true),
    ('11c088b6-31e7-4173-aa0f-367b1715e54f', 'ad52c551-01c1-4c7f-9a96-720eecb32885', '12', 'terminal_type.12', 8, '手机端Android', true),
    ('d264c01e-072e-46fc-b607-88061810c9b6', 'ad52c551-01c1-4c7f-9a96-720eecb32885', '16', 'terminal_type.16', 9, '手机端iOS', true);

-- access_rule_type
merge into "sys_dict_item" ("id", "dict_id", "item_code", "item_name", "order_num", "remark", "built_in") values
    ('1264c011-172e-16fc-b607-28061810c9b1', 'ad52c541-02c1-3c7f-1a96-a20eecb32881', '0', 'access_rule_type.0', 1, '不限制', true),
    ('1264c011-172e-16fc-b607-28061810c9b2', 'ad52c541-02c1-3c7f-1a96-a20eecb32881', '1', 'access_rule_type.1', 2, '白名单', true),
    ('1264c011-172e-16fc-b607-28061810c9b3', 'ad52c541-02c1-3c7f-1a96-a20eecb32881', '2', 'access_rule_type.2', 2, '黑名单', true),
    ('1264c011-172e-16fc-b607-28061810c9b4', 'ad52c541-02c1-3c7f-1a96-a20eecb32881', '3', 'access_rule_type.3', 2, '白名单+黑名单', true);

--endregion DML
