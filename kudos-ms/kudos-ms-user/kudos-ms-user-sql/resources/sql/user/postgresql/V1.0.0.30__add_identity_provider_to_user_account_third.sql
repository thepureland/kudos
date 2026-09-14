-- Add the tenant provider-instance dimension required by the federated identity key
-- (providerId + issuer + subject). Existing rows remain readable through the legacy provider code.
alter table "user_account_third"
    add column if not exists "identity_provider_id" character varying(36);

alter table "user_account_third"
    alter column "account_provider_issuer" type character varying(512);

comment on column "user_account_third"."identity_provider_id" is
    'auth_identity_provider.id；历史绑定可为空';

create unique index if not exists "uq_user_account_third__tenant_idp_issuer_subject"
    on "user_account_third" ("tenant_id", "identity_provider_id", "account_provider_issuer", "subject");
