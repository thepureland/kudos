-- These provider-instance constraints remain effective even when a non-OIDC provider has no issuer.
-- They are the final concurrency guard after the service's account-scoped lock and preflight checks.
create unique index if not exists "uq_user_account_third__tenant_idp_subject"
    on "user_account_third" ("tenant_id", "identity_provider_id", "subject");

create unique index if not exists "uq_user_account_third__user_idp"
    on "user_account_third" ("user_id", "identity_provider_id");

create table if not exists "user_account_third_audit"
(
    "id"                   character(36) default gen_random_uuid()::text not null primary key,
    "binding_id"           character varying(36),
    "user_id"              character varying(36)  not null,
    "tenant_id"            character varying(36)  not null,
    "identity_provider_id" character varying(36),
    "provider_code"        character varying(32)  not null,
    "subject_hash"         character(64)           not null,
    "action"               character varying(16)  not null,
    "success"              boolean                not null,
    "reason"               character varying(128),
    "actor_user_id"        character varying(36)  not null,
    "event_time"           timestamp(6)           not null
);

create index if not exists "idx_user_account_third_audit_user_time"
    on "user_account_third_audit" ("tenant_id", "user_id", "event_time");

create index if not exists "idx_user_account_third_audit_provider_time"
    on "user_account_third_audit" ("identity_provider_id", "event_time");

comment on table "user_account_third_audit" is '第三方身份绑定/解绑追加式审计日志';
comment on column "user_account_third_audit"."subject_hash" is '外部 subject 的 SHA-256，审计表不保存原值';
