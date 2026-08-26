CREATE TABLE IF NOT EXISTS "auth_credential_revocation" (
    "id" VARCHAR(36) NOT NULL,
    "tenant_id" VARCHAR(36) NOT NULL,
    "user_id" VARCHAR(36) NOT NULL,
    "credential_type" VARCHAR(16) NOT NULL,
    -- The WebAuthn row that was revoked, plus its audit fingerprint. Never the raw credential id: the audit
    -- view does not expose it either, and an administrator has no reason to hold one.
    "credential_ref" VARCHAR(36),
    "credential_fingerprint" VARCHAR(64),
    "actor_user_id" VARCHAR(36) NOT NULL,
    "reason" VARCHAR(512) NOT NULL,
    "security_event_id" VARCHAR(36),
    -- What the account was left with, recorded at the moment of revocation so the decision can be judged later
    -- on what was actually known then.
    "left_without_factor" BOOLEAN NOT NULL,
    "revoked_at" TIMESTAMP NOT NULL,
    CONSTRAINT "pk_auth_credential_revocation" PRIMARY KEY ("id"),
    CONSTRAINT "fk_auth_credential_revocation_event" FOREIGN KEY ("security_event_id")
        REFERENCES "auth_security_event" ("id"),
    CONSTRAINT "ck_auth_credential_revocation_type" CHECK ("credential_type" IN ('WEBAUTHN', 'TOTP')),
    CONSTRAINT "ck_auth_credential_revocation_reason" CHECK (CHAR_LENGTH("reason") > 0),
    -- A WebAuthn revocation names one credential; a TOTP authenticator is singular per account and has none.
    CONSTRAINT "ck_auth_credential_revocation_ref" CHECK (
        ("credential_type" = 'WEBAUTHN' AND "credential_ref" IS NOT NULL AND "credential_fingerprint" IS NOT NULL)
        OR ("credential_type" = 'TOTP' AND "credential_ref" IS NULL AND "credential_fingerprint" IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS "idx_auth_credential_revocation_user"
    ON "auth_credential_revocation" ("tenant_id", "user_id", "revoked_at");

CREATE INDEX IF NOT EXISTS "idx_auth_credential_revocation_event"
    ON "auth_credential_revocation" ("tenant_id", "security_event_id");

COMMENT ON TABLE "auth_credential_revocation" IS
    '管理员发起的凭证吊销；追加式，记录操作者、原因、可选关联安全事件及吊销后账号是否已无可用因子';
COMMENT ON COLUMN "auth_credential_revocation"."left_without_factor" IS
    '吊销后该账号是否已不再拥有任何已注册 MFA 因子；用于事后判断是否需要注册豁免救援';
