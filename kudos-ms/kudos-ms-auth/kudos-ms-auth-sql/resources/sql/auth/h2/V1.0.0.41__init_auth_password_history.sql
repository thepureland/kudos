-- Retired password hashes used to reject credential reuse. Raw passwords are never persisted.
create table if not exists "auth_password_history"
(
    "id"            character(36)          not null primary key,
    "tenant_id"     character varying(36)  not null,
    "user_id"       character varying(36)  not null,
    "purpose"       character varying(32)  not null,
    "password_hash" character varying(255) not null,
    "recorded_at"   timestamp(6)           not null
);

create index if not exists "idx_auth_password_history_account"
    on "auth_password_history" ("tenant_id", "user_id", "purpose", "recorded_at");

comment on table "auth_password_history" is '账号历史密码哈希（禁止密码复用）';
comment on column "auth_password_history"."purpose" is '密码用途：LOGIN或SECURITY';
comment on column "auth_password_history"."password_hash" is '已退役的单向密码哈希，禁止存储明文';
