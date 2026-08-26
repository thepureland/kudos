CREATE TABLE IF NOT EXISTS "auth_webauthn_authenticator_risk_policy" (
    "id" VARCHAR(36) NOT NULL,
    "tenant_id" VARCHAR(36) NOT NULL,
    "blocked_risk_levels" CLOB,
    "create_user_id" VARCHAR(36) NOT NULL,
    "create_reason" VARCHAR(512) NOT NULL,
    "create_time" TIMESTAMP NOT NULL,
    "update_user_id" VARCHAR(36) NOT NULL,
    "update_reason" VARCHAR(512) NOT NULL,
    "update_time" TIMESTAMP NOT NULL,
    CONSTRAINT "pk_auth_webauthn_authenticator_risk_policy" PRIMARY KEY ("id"),
    CONSTRAINT "uk_auth_webauthn_authenticator_risk_policy_tenant" UNIQUE ("tenant_id"),
    CONSTRAINT "ck_auth_webauthn_authenticator_risk_policy_id_tenant" CHECK ("id" = "tenant_id")
);
