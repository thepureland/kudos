create table if not exists "auth_identity_provider_jit_config"
(
    "id"                     character(36) not null primary key,
    "tenant_id"              character varying(36) not null,
    "username_strategy"      character varying(32) default 'EXTERNAL_USERNAME_HASHED' not null,
    "require_verified_email" boolean default false not null,
    "allowed_email_domains"  text,
    "default_org_id"         character varying(128),
    "default_supervisor_id"  character varying(36),
    "account_type_dict_code" character varying(5),
    "account_status_dict_code" character varying(5),
    "default_locale"         character varying(5),
    "default_timezone"       character varying(64),
    "default_currency"       character varying(3),
    "create_user_id"         character varying(36) not null,
    "create_reason"          character varying(512) not null,
    "create_time"            timestamp(6) not null,
    "update_user_id"         character varying(36) not null,
    "update_reason"          character varying(512) not null,
    "update_time"            timestamp(6) not null,
    constraint "fk_auth_identity_provider_jit_config_provider" foreign key ("id")
        references "auth_identity_provider" ("id"),
    constraint "ck_auth_identity_provider_jit_username_strategy" check (
        "username_strategy" in ('EXTERNAL_USERNAME_HASHED', 'EMAIL_LOCAL_PART_HASHED', 'OPAQUE_HASHED')
    )
);

create index if not exists "idx_auth_identity_provider_jit_config_tenant"
    on "auth_identity_provider_jit_config" ("tenant_id");

comment on table "auth_identity_provider_jit_config" is '租户 Provider 实例的 JIT 自动开户默认值';
comment on column "auth_identity_provider_jit_config"."allowed_email_domains" is
    '规范化 ASCII 邮箱域名 CSV，支持 *.example.com；配置后强制要求上游 verified email';
