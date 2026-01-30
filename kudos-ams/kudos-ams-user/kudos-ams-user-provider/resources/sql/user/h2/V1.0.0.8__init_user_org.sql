--region DDL
create table if not exists "user_org"
(
    "id"          character(36) default RANDOM_UUID() not null primary key,
    "name"        character varying(64)  not null,
    "short_name"        character varying(32),
    "tenant_id"   character varying(36)  not null,
    "parent_id"   character varying(36),
    "org_type_dict_code" varchar(32)  NOT NULL,
    "sort_num"    integer default 0,
    "remark"      character varying(256),
    "active"      boolean default TRUE   not null,
    "built_in"    boolean default FALSE,
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6),
    constraint "uk_user_org_tenant_name" unique ("tenant_id", "name")
    );

comment on table "user_org" is '机构';
comment on column "user_org"."id" is '主键';
comment on column "user_org"."name" is '机构名称';
comment on column "user_org"."short_name" is '机构简称';
comment on column "user_org"."tenant_id" is '租户id';
comment on column "user_org"."parent_id" is '父机构id';
comment on column "user_org"."org_type_dict_code" is '机构类型字码典';
comment on column "user_org"."sort_num" is '排序号';
comment on column "user_org"."remark" is '备注';
comment on column "user_org"."active" is '是否激活';
comment on column "user_org"."built_in" is '是否内置';
comment on column "user_org"."create_user_id" is '创建者id';
comment on column "user_org"."create_user_name" is '创建者名称';
comment on column "user_org"."create_time" is '创建时间';
comment on column "user_org"."update_user_id" is '更新者id';
comment on column "user_org"."update_user_name" is '更新者名称';
comment on column "user_org"."update_time" is '更新时间';

--endregion DDL


--region DML

--endregion DML
