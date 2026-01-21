--region DDL
create table if not exists "auth_dept_user"
(
    "id"          character(36) default RANDOM_UUID() not null primary key,
    "dept_id"     character(36)  not null,
    "user_id"     character(36)  not null,
    "dept_admin" boolean default FALSE   not null,
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6),
    constraint "uk_auth_dept_user" unique ("dept_id", "user_id")
    );

-- 创建索引以提升查询性能
create index if not exists "idx_auth_dept_user_dept_id" on "auth_dept_user" ("dept_id");
create index if not exists "idx_auth_dept_user_user_id" on "auth_dept_user" ("user_id");

comment on table "auth_dept_user" is '部门-用户';
comment on column "auth_dept_user"."id" is '主键';
comment on column "auth_dept_user"."dept_id" is '部门id';
comment on column "auth_dept_user"."user_id" is '用户id';
comment on column "auth_dept_user"."dept_admin" is '是否为部门管理员';
comment on column "auth_dept_user"."create_user_id" is '创建者id';
comment on column "auth_dept_user"."create_user_name" is '创建者名称';
comment on column "auth_dept_user"."create_time" is '创建时间';
comment on column "auth_dept_user"."update_user_id" is '更新者id';
comment on column "auth_dept_user"."update_user_name" is '更新者名称';
comment on column "auth_dept_user"."update_time" is '更新时间';

--endregion DDL


--region DML

--endregion DML
