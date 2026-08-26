-- Verified WebAuthn public-key credentials. No private-key or challenge material is stored here.
create table if not exists "auth_webauthn_credential"
(
    "id"                 character(36)           not null primary key,
    "tenant_id"          character varying(36)   not null,
    "user_id"            character varying(36)   not null,
    "credential_id"      character varying(2048) not null,
    "user_handle"        character varying(128)  not null,
    "public_key_cose"    character varying(8192) not null,
    "signature_count"    bigint                  not null default 0,
    "transports"         character varying(64),
    "aaguid"             character varying(36),
    "attestation_format" character varying(32),
    "backup_eligible"    boolean                 not null default false,
    "backed_up"          boolean                 not null default false,
    "discoverable"       boolean                 not null default false,
    "display_name"       character varying(100)  not null,
    "created_at"         timestamp(6)            not null,
    "last_used_at"       timestamp(6),
    "revoked_at"         timestamp(6),
    "version"            bigint                  not null default 0,
    constraint "uk_auth_webauthn_credential" unique ("tenant_id", "credential_id")
);

create index if not exists "idx_auth_webauthn_credential_account"
    on "auth_webauthn_credential" ("tenant_id", "user_id", "revoked_at", "created_at");

create index if not exists "idx_auth_webauthn_credential_user_handle"
    on "auth_webauthn_credential" ("tenant_id", "user_handle", "revoked_at");

comment on table "auth_webauthn_credential" is 'WebAuthn凭证（仅服务端公钥材料及认证器状态）';
comment on column "auth_webauthn_credential"."credential_id" is 'Base64url无填充的凭证ID';
comment on column "auth_webauthn_credential"."public_key_cose" is 'Base64url无填充的COSE公钥，不含私钥';
comment on column "auth_webauthn_credential"."signature_count" is '认证器签名计数器，使用CAS推进以检测重放/克隆风险';
