CREATE TABLE IF NOT EXISTS "auth_credential" (
    "id" VARCHAR(36) NOT NULL,
    "tenant_id" VARCHAR(36) NOT NULL,
    "user_id" VARCHAR(36) NOT NULL,
    "type" VARCHAR(32) NOT NULL,
    -- Either the encoded secret itself (a versioned `{bcrypt}` hash, an AES-GCM ciphertext) or a reference to
    -- one held elsewhere. Never a plaintext secret, and never returned by any read this service exposes.
    "secret_hash_or_ref" VARCHAR(1024) NOT NULL,
    "status" VARCHAR(16) NOT NULL,
    "version" BIGINT NOT NULL DEFAULT 0,
    "enrolled_at" TIMESTAMP NOT NULL,
    "expires_at" TIMESTAMP,
    "last_used_at" TIMESTAMP,
    -- Non-secret descriptive attributes only (algorithm hints, issuer labels). Bounded so it cannot become a
    -- second place where secrets accumulate.
    "metadata" VARCHAR(1024),
    "revoked_at" TIMESTAMP,
    "revoke_reason" VARCHAR(512),
    "create_time" TIMESTAMP NOT NULL,
    "update_time" TIMESTAMP NOT NULL,
    CONSTRAINT "pk_auth_credential" PRIMARY KEY ("id"),
    -- One active credential of a kind per account. Rotation replaces the row's secret under its version,
    -- rather than accumulating rows whose "current one" has to be guessed at read time.
    CONSTRAINT "uk_auth_credential_active" UNIQUE ("tenant_id", "user_id", "type", "status"),
    CONSTRAINT "ck_auth_credential_type" CHECK ("type" IN ('PASSWORD', 'SECURITY_PASSWORD', 'TOTP')),
    CONSTRAINT "ck_auth_credential_status" CHECK ("status" IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT "ck_auth_credential_secret" CHECK (CHAR_LENGTH("secret_hash_or_ref") > 0),
    CONSTRAINT "ck_auth_credential_version" CHECK ("version" >= 0),
    CONSTRAINT "ck_auth_credential_revocation" CHECK (
        ("status" = 'ACTIVE' AND "revoked_at" IS NULL AND "revoke_reason" IS NULL)
        OR ("status" = 'REVOKED' AND "revoked_at" IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS "idx_auth_credential_user"
    ON "auth_credential" ("tenant_id", "user_id", "type", "status");

COMMENT ON TABLE "auth_credential" IS
    '认证凭证的最终归属表；密码/安全密码/TOTP 的编码后秘密或引用，WebAuthn 与恢复码另有专表';
COMMENT ON COLUMN "auth_credential"."secret_hash_or_ref" IS '编码后的秘密或其引用，绝不保存明文，也不通过任何读接口返回';
COMMENT ON COLUMN "auth_credential"."version" IS '乐观并发版本；轮换以旧版本 CAS，防止并发改密互相覆盖';
COMMENT ON COLUMN "auth_credential"."metadata" IS '仅非秘密的描述性属性，长度受限以免成为第二个秘密堆积处';
