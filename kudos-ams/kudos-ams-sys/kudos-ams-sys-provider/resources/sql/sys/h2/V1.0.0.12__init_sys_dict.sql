--region DDL
create table if not exists "sys_dict"
(
    "id"          character(36) default RANDOM_UUID() not null primary key,
    "dict_type"   character varying(64)               not null,
    "dict_name"   character varying(64)               not null,
    "atomic_service_code" character varying(32)               not null,
    "remark"      character varying(300),
    "active"      boolean       default TRUE          not null,
    "built_in"    boolean       default FALSE         not null,
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6)     default now(),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6)
);

create unique index if not exists "uq_sys_dict" on "sys_dict" ("dict_type", "atomic_service_code");

-- alter table "sys_dict"
--     add constraint "fk_sys_dict_module"
--         foreign key ("atomic_service_code") references "sys_module" ("code");

comment on table "sys_dict" is '字典';
comment on column "sys_dict"."id" is '主键';
comment on column "sys_dict"."dict_type" is '字典类型';
comment on column "sys_dict"."dict_name" is '字典名称或其国际化key';
comment on column "sys_dict"."atomic_service_code" is '原子服务编码';
comment on column "sys_dict"."remark" is '备注';
comment on column "sys_dict"."active" is '是否启用';
comment on column "sys_dict"."built_in" is '是否内置';
comment on column "sys_dict"."create_user_id" is '创建者id';
comment on column "sys_dict"."create_user_name" is '创建者名称';
comment on column "sys_dict"."create_time" is '创建时间';
comment on column "sys_dict"."update_user_id" is '更新者id';
comment on column "sys_dict"."update_user_name" is '更新者名称';
comment on column "sys_dict"."update_time" is '更新时间';
--endregion DDL


--region DML

merge into "sys_dict" ("id", "atomic_service_code", "dict_type", "dict_name", "remark", "built_in") values
    ('68139ed2-dbce-47fa-ac0d-2932fb0ee5ad', 'kudos-sys', 'ds_use', 'ds_use', null, false),
    ('d9f17338-8751-4d3b-bdd1-91a1b6f42432', 'kudos-sys', 'ds_type', 'ds_type', '暂时只支持一种数据源类型hikariCP', false),
    ('339b4cf1-6af4-49db-be1c-ee606959a689', 'kudos-sys', 'resource_type', 'resource_type', null, false),
    ('2601c57f-3900-4be8-9ebf-e79781db9d3d', 'kudos-sys', 'cache_strategy', 'cache_strategy', null, false),
    ('54094f46-dddb-41a2-b747-0eaa7d0e59b6', 'kudos-sys', 'locale', 'locale', 'locale', true),
    ('54094f46-dddb-41a2-b747-0eaa7d0ekil6', 'kudos-sys', 'i18n_type', 'i18n_type', null, true),
    ('1b87ef01-c033-06a6-0525-b317b623899f', 'kudos-sys', 'timezone', 'timezone', null, false),
    ('e960b247-16e0-4f4e-a767-2b17eb5b6982', 'kudos-sys', 'domain_type', 'domain_type', null, true),
    ('ad52c551-01c1-4c7f-9a96-720eecb32885', 'kudos-sys', 'terminal_type', 'terminal_type', null, true),
    ('ad52c541-02c1-3c7f-1a96-a20eecb32881', 'kudos-sys', 'access_rule_type', 'access_rule_type', null, true);

--endregion DML


