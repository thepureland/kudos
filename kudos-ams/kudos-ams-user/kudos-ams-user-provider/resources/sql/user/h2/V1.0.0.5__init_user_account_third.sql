--region DDL
create table if not exists "user_account_third"
(
    "id"                    char(36)    default RANDOM_UUID() not null primary key,
    "user_account_id"       char(36)                          not null,
    "account_provider_dict_code"    varchar(32)                       not null,
    "account_provider_issuer"       varchar(128),
    "subject"               varchar(255)                      not null,
    "union_id"              varchar(255),
    "external_display_name" varchar(128),
    "external_email"        varchar(254),
    "avatar_url"            varchar(512),
    "last_login_time"       timestamptz,
    "tenant_id"             varchar(36)                       not null,
    "remark"                varchar(255),
    "active"                boolean     default true          not null,
    "built_in"              boolean     default false         not null,
    "create_user_id" character varying(36),
    "create_user_name" character varying(32),
    "create_time" timestamp(6),
    "update_user_id" character varying(36),
    "update_user_name" character varying(32),
    "update_time" timestamp(6),
    constraint "fk_user_account_third__user_account"
        foreign key ("user_account_id") references "user_account" ("id")
            on delete cascade
);

comment on table "user_account_third" is '第三方账号';
comment on column "user_account_third"."id" is '主键';
comment on column "user_account_third"."user_account_id" is '系统账号的ID';
comment on column "user_account_third"."account_provider_dict_code" is '第三方平台/提供方代码';
comment on column "user_account_third"."account_provider_issuer" is '发行方/平台租户标识';
comment on column "user_account_third"."subject" is '第三方用户唯一标识';
comment on column "user_account_third"."union_id" is '跨应用统一标识';
comment on column "user_account_third"."external_display_name" is '第三方展示名';
comment on column "user_account_third"."external_email" is '第三方邮箱';
comment on column "user_account_third"."avatar_url" is '第三方头像 URL';
comment on column "user_account_third"."last_login_time" is '该第三方渠道最后一次成功登录时间';
comment on column "user_account_third"."tenant_id" is '租户ID';
comment on column "user_account_third"."remark" is '备注';
comment on column "user_account_third"."active" is '是否启用ID';
comment on column "user_account_third"."built_in" is '是否内置';
comment on column "user_account"."create_user_id" is '创建者ID';
comment on column "user_account"."create_user_name" is '创建者名称';
comment on column "user_account"."create_time" is '创建时间';
comment on column "user_account"."update_user_id" is '更新者ID';
comment on column "user_account"."update_user_name" is '更新者名称';
comment on column "user_account"."update_time" is '更新时间';


-- 同租户下：同一第三方身份只能绑定一次
create unique index "uq_user_account_third__tenant_provider_issuer_subject"
    on "user_account_third" ("tenant_id", "account_provider_dict_code", "account_provider_issuer", "subject");

-- 常用查询：查某账号绑定了哪些第三方
create index "ix_user_account_third__user_account_id"
    on "user_account_third" ("user_account_id");

-- 常用查询：按租户查第三方绑定（登录回调落库/查重）
create index "ix_user_account_third__tenant_provider_subject"
    on "user_account_third" ("tenant_id", "account_provider_dict_code", "subject");
--endregion DDL


--region DML

--endregion DML
