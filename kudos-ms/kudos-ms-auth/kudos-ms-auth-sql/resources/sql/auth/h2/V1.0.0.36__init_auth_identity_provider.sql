--region DDL
create table if not exists "auth_provider_template"
(
    "id"                    character(36) default RANDOM_UUID() not null primary key,
    "code"                  character varying(32)  not null,
    "protocol"              character varying(16)  not null,
    "issuer"                character varying(512),
    "discovery_uri"         character varying(512),
    "authorization_uri"     character varying(512),
    "token_uri"             character varying(512),
    "user_info_uri"         character varying(512),
    "jwk_set_uri"           character varying(512),
    "subject_claim"         character varying(64) default 'sub' not null,
    "default_scopes"        character varying(512),
    "adapter_type"          character varying(64),
    "default_claim_mapping" text,
    "logo_uri"              character varying(512),
    "active"                boolean default true not null,
    "create_time"           timestamp(6),
    "update_time"           timestamp(6),
    constraint "uq_auth_provider_template_code" unique ("code")
);

create table if not exists "auth_identity_provider"
(
    "id"                character(36) default RANDOM_UUID() not null primary key,
    "tenant_id"         character varying(36)  not null,
    "template_id"       character varying(36)  not null,
    "code"              character varying(32)  not null,
    "display_name"      character varying(128) not null,
    "issuer"            character varying(512),
    "client_id"         character varying(256) not null,
    "client_secret_ref" character varying(512),
    "scopes"            character varying(512),
    "custom_config"     text,
    "jit_policy"        character varying(32) default 'DISABLED' not null,
    "link_policy"       character varying(32) default 'BOUND_ONLY' not null,
    "active"            boolean default true not null,
    "create_time"       timestamp(6),
    "update_time"       timestamp(6),
    constraint "fk_auth_identity_provider_template" foreign key ("template_id")
        references "auth_provider_template" ("id"),
    constraint "uq_auth_identity_provider_tenant_code" unique ("tenant_id", "code")
);

create index if not exists "idx_auth_identity_provider_tenant_active"
    on "auth_identity_provider" ("tenant_id", "active");

comment on table "auth_provider_template" is '外部身份提供方平台模板';
comment on table "auth_identity_provider" is '租户外部身份提供方实例';
comment on column "auth_identity_provider"."client_secret_ref" is 'Vault/KMS/环境配置引用，禁止存储明文 secret';
--endregion DDL

--region DML
-- The fixed endpoint values below come from each provider's official OIDC discovery document.
merge into "auth_provider_template" (
    "id", "code", "protocol", "issuer", "discovery_uri", "authorization_uri", "token_uri",
    "user_info_uri", "jwk_set_uri", "subject_claim", "default_scopes", "adapter_type", "active"
) key ("code") values (
    'a0010000-0000-0000-0000-000000000001', 'GOOGLE', 'OIDC',
    'https://accounts.google.com', 'https://accounts.google.com/.well-known/openid-configuration',
    'https://accounts.google.com/o/oauth2/v2/auth', 'https://oauth2.googleapis.com/token',
    'https://openidconnect.googleapis.com/v1/userinfo', 'https://www.googleapis.com/oauth2/v3/certs',
    'sub', 'openid profile email', 'STANDARD_OIDC', true
);

merge into "auth_provider_template" (
    "id", "code", "protocol", "issuer", "discovery_uri", "authorization_uri", "token_uri",
    "user_info_uri", "jwk_set_uri", "subject_claim", "default_scopes", "adapter_type", "active"
) key ("code") values (
    'a0010000-0000-0000-0000-000000000002', 'LINE', 'OIDC',
    'https://access.line.me', 'https://access.line.me/.well-known/openid-configuration',
    'https://access.line.me/oauth2/v2.1/authorize', 'https://api.line.me/oauth2/v2.1/token',
    'https://api.line.me/oauth2/v2.1/userinfo', 'https://api.line.me/oauth2/v2.1/certs',
    'sub', 'openid profile email', 'STANDARD_OIDC', true
);

merge into "auth_provider_template" (
    "id", "code", "protocol", "subject_claim", "default_scopes", "adapter_type", "active"
) key ("code") values (
    'a0010000-0000-0000-0000-000000000003', 'GENERIC_OIDC', 'OIDC',
    'sub', 'openid profile email', 'STANDARD_OIDC', true
);
--endregion DML
