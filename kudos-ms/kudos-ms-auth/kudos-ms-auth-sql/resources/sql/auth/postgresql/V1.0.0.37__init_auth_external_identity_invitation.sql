create table if not exists "auth_external_identity_invitation"
(
    "id"                      character(36) default gen_random_uuid()::text not null primary key,
    "tenant_id"               character varying(36)  not null,
    "user_id"                 character varying(36)  not null,
    "identity_provider_id"    character varying(36)  not null,
    "token_hash"              character(64)          not null,
    "expected_email_hash"     character(64),
    "max_uses"                integer default 1      not null,
    "used_count"              integer default 0      not null,
    "expires_at"              timestamp(6)           not null,
    "active"                  boolean default true   not null,
    "create_user_id"          character varying(36)  not null,
    "create_reason"           character varying(512) not null,
    "create_time"             timestamp(6)           not null,
    "revoke_user_id"          character varying(36),
    "revoke_reason"           character varying(512),
    "revoke_time"             timestamp(6),
    "last_used_time"          timestamp(6),
    "consumed_subject_hash"   character(64),
    "update_time"             timestamp(6),
    constraint "fk_auth_external_invitation_provider" foreign key ("identity_provider_id")
        references "auth_identity_provider" ("id"),
    constraint "uq_auth_external_invitation_token_hash" unique ("token_hash"),
    constraint "ck_auth_external_invitation_uses"
        check ("max_uses" > 0 and "used_count" >= 0 and "used_count" <= "max_uses")
);

create index if not exists "idx_auth_external_invitation_scope"
    on "auth_external_identity_invitation" ("tenant_id", "identity_provider_id", "active", "expires_at");

comment on table "auth_external_identity_invitation" is '外部身份一次性邀请；令牌、预期邮箱和已消费 subject 仅保存 SHA-256';
comment on column "auth_external_identity_invitation"."token_hash" is '一次性 bearer token 的 SHA-256，原文只在创建响应返回';
comment on column "auth_external_identity_invitation"."expected_email_hash" is '可选的预期已验证邮箱规范化值 SHA-256';
comment on column "auth_external_identity_invitation"."consumed_subject_hash" is '成功消费的外部 subject SHA-256';
