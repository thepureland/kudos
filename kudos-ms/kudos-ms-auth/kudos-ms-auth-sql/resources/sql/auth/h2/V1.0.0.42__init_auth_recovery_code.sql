-- Hash-only, single-use recovery codes grouped into replaceable sets.
create table if not exists "auth_recovery_code"
(
    "id"          character(36)          not null primary key,
    "tenant_id"   character varying(36)  not null,
    "user_id"     character varying(36)  not null,
    "set_id"      character(36)          not null,
    "code_hash"   character(64)          not null,
    "created_at"  timestamp(6)           not null,
    "consumed_at" timestamp(6),
    "revoked_at"  timestamp(6),
    constraint "uk_auth_recovery_code_hash" unique ("code_hash")
);

create index if not exists "idx_auth_recovery_code_account"
    on "auth_recovery_code" ("tenant_id", "user_id", "revoked_at", "created_at");

comment on table "auth_recovery_code" is 'MFA恢复码（仅保存SHA-256，单次使用，整组轮换）';
comment on column "auth_recovery_code"."code_hash" is '租户、用户、恢复码组和高熵恢复码的SHA-256';
