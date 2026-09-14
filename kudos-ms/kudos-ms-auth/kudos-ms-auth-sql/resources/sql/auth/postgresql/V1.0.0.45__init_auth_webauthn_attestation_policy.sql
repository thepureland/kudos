CREATE TABLE IF NOT EXISTS "auth_webauthn_attestation_policy" (
    "id" VARCHAR(36) NOT NULL,
    "tenant_id" VARCHAR(36) NOT NULL,
    "aaguid_mode" VARCHAR(16) NOT NULL,
    "aaguids" text,
    "allowed_attestation_formats" text,
    "require_trusted_attestation" BOOLEAN NOT NULL,
    "create_user_id" VARCHAR(36) NOT NULL,
    "create_reason" VARCHAR(512) NOT NULL,
    "create_time" TIMESTAMP NOT NULL,
    "update_user_id" VARCHAR(36) NOT NULL,
    "update_reason" VARCHAR(512) NOT NULL,
    "update_time" TIMESTAMP NOT NULL,
    CONSTRAINT "pk_auth_webauthn_attestation_policy" PRIMARY KEY ("id"),
    CONSTRAINT "uk_auth_webauthn_attestation_policy_tenant" UNIQUE ("tenant_id"),
    CONSTRAINT "ck_auth_webauthn_attestation_policy_id_tenant" CHECK ("id" = "tenant_id"),
    CONSTRAINT "ck_auth_webauthn_attestation_policy_mode"
        CHECK ("aaguid_mode" IN ('NONE', 'ALLOW_LIST', 'DENY_LIST'))
);
