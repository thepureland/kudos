--region DDL
create table if not exists "user_org_user"
(
    "id"          character(36) default RANDOM_UUID() not null primary key,
    "org_id"     character(36)  not null,
    "user_id"     character(36)  not null,
    "org_admin" boolean default FALSE   not null,
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6),
    constraint "uk_user_org_user" unique ("org_id", "user_id")
    );

-- 创建索引以提升查询性能
create index if not exists "idx_user_org_user_org_id" on "user_org_user" ("org_id");
create index if not exists "idx_user_org_user_user_id" on "user_org_user" ("user_id");

comment on table "user_org_user" is '机构-用户';
comment on column "user_org_user"."id" is '主键';
comment on column "user_org_user"."org_id" is '机构id';
comment on column "user_org_user"."user_id" is '用户id';
comment on column "user_org_user"."org_admin" is '是否为机构管理员';
comment on column "user_org_user"."create_user_id" is '创建者id';
comment on column "user_org_user"."create_user_name" is '创建者名称';
comment on column "user_org_user"."create_time" is '创建时间';
comment on column "user_org_user"."update_user_id" is '更新者id';
comment on column "user_org_user"."update_user_name" is '更新者名称';
comment on column "user_org_user"."update_time" is '更新时间';

--endregion DDL


--region DML

--endregion DML
