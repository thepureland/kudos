--region DDL
create table if not exists "auth_dept"
(
    "id"          character(36) default RANDOM_UUID() not null primary key,
    "name"        character varying(64)  not null,
    "short_name"        character varying(32),
    "tenant_id"   character varying(36)  not null,
    "parent_id"   character varying(36),
    "dept_type_dict_code" varchar(32)  NOT NULL,
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
    constraint "uk_auth_dept_tenant_name" unique ("tenant_id", "name")
    );

comment on table "auth_dept" is '部门';
comment on column "auth_dept"."id" is '主键';
comment on column "auth_dept"."name" is '部门名称';
comment on column "auth_dept"."short_name" is '部门简称';
comment on column "auth_dept"."tenant_id" is '租户id';
comment on column "auth_dept"."parent_id" is '父部门id';
comment on column "auth_dept"."dept_type_dict_code" is '部门类型字码典';
comment on column "auth_dept"."sort_num" is '排序号';
comment on column "auth_dept"."remark" is '备注';
comment on column "auth_dept"."active" is '是否激活';
comment on column "auth_dept"."built_in" is '是否内置';
comment on column "auth_dept"."create_user_id" is '创建者id';
comment on column "auth_dept"."create_user_name" is '创建者名称';
comment on column "auth_dept"."create_time" is '创建时间';
comment on column "auth_dept"."update_user_id" is '更新者id';
comment on column "auth_dept"."update_user_name" is '更新者名称';
comment on column "auth_dept"."update_time" is '更新时间';

--endregion DDL


--region DML

--endregion DML
