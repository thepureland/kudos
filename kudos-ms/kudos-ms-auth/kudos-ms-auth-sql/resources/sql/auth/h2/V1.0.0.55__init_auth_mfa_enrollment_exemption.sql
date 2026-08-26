CREATE TABLE IF NOT EXISTS "auth_mfa_enrollment_exemption" (
    "id" VARCHAR(36) NOT NULL,
    "tenant_id" VARCHAR(36) NOT NULL,
    "user_id" VARCHAR(36) NOT NULL,
    "status" VARCHAR(16) NOT NULL,
    "reason" VARCHAR(512) NOT NULL,
    "granted_by" VARCHAR(36) NOT NULL,
    "granted_at" TIMESTAMP NOT NULL,
    "expires_at" TIMESTAMP NOT NULL,
    "revoked_by" VARCHAR(36),
    "revoke_reason" VARCHAR(512),
    "revoked_at" TIMESTAMP,
    CONSTRAINT "pk_auth_mfa_enrollment_exemption" PRIMARY KEY ("id"),
    CONSTRAINT "ck_auth_mfa_enrollment_exemption_status" CHECK ("status" IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT "ck_auth_mfa_enrollment_exemption_reason" CHECK (CHAR_LENGTH("reason") > 0),
    CONSTRAINT "ck_auth_mfa_enrollment_exemption_window" CHECK ("granted_at" < "expires_at"),
    -- Revocation fields are all-or-nothing, so a revoked grant can never be missing who did it or why.
    CONSTRAINT "ck_auth_mfa_enrollment_exemption_revocation" CHECK (
        ("status" = 'ACTIVE' AND "revoked_by" IS NULL AND "revoke_reason" IS NULL AND "revoked_at" IS NULL)
        OR ("status" = 'REVOKED' AND "revoked_by" IS NOT NULL AND "revoke_reason" IS NOT NULL
            AND "revoked_at" IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS "idx_auth_mfa_enrollment_exemption_user"
    ON "auth_mfa_enrollment_exemption" ("tenant_id", "user_id", "status", "expires_at");

COMMENT ON TABLE "auth_mfa_enrollment_exemption" IS
    '逐用户 MFA 注册要求的临时豁免；只放行"尚未注册"这一阻断，不豁免已注册用户的第二因素校验';
COMMENT ON COLUMN "auth_mfa_enrollment_exemption"."expires_at" IS '强制到期时间，由服务层按上限收敛';
COMMENT ON COLUMN "auth_mfa_enrollment_exemption"."status" IS 'ACTIVE 或被管理员提前撤销的 REVOKED；每次授予都是新行，历史保留';
