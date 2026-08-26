CREATE TABLE IF NOT EXISTS "auth_login_event" (
    "id" VARCHAR(36) NOT NULL,
    "tenant_id" VARCHAR(36),
    "user_id" VARCHAR(36),
    -- Never the raw identifier: a failed attempt for an unknown account still names somebody, and the audit
    -- trail must stay useful without becoming a list of guessable usernames.
    "identifier_hash" VARCHAR(64),
    "provider_id" VARCHAR(36),
    "authentication_method" VARCHAR(64),
    "transaction_id" VARCHAR(36) NOT NULL,
    "purpose" VARCHAR(16) NOT NULL,
    "session_id" VARCHAR(64),
    "success" BOOLEAN NOT NULL,
    "failure_code" VARCHAR(64),
    "acr" VARCHAR(64),
    "amr" VARCHAR(128),
    "login_ip" BIGINT,
    "login_location" VARCHAR(128),
    "login_device" VARCHAR(64),
    "login_browser" VARCHAR(64),
    "login_os" VARCHAR(64),
    "user_agent" VARCHAR(512),
    "occurred_at" TIMESTAMP NOT NULL,
    CONSTRAINT "pk_auth_login_event" PRIMARY KEY ("id"),
    -- One row per transaction outcome. The transaction can only turn terminal once, so this both expresses the
    -- intent and makes a duplicated feed impossible rather than merely unlikely.
    CONSTRAINT "uk_auth_login_event_transaction" UNIQUE ("transaction_id"),
    CONSTRAINT "ck_auth_login_event_purpose" CHECK ("purpose" IN ('LOGIN', 'STEP_UP', 'LINK_EXTERNAL_IDENTITY')),
    -- Success and failure carry different evidence; neither may borrow the other's.
    CONSTRAINT "ck_auth_login_event_outcome" CHECK (
        ("success" = TRUE AND "failure_code" IS NULL AND "user_id" IS NOT NULL)
        OR ("success" = FALSE AND "failure_code" IS NOT NULL)
    ),
    CONSTRAINT "ck_auth_login_event_identifier_hash" CHECK (
        "identifier_hash" IS NULL OR CHAR_LENGTH("identifier_hash") = 64
    )
);

CREATE INDEX IF NOT EXISTS "idx_auth_login_event_tenant_time"
    ON "auth_login_event" ("tenant_id", "occurred_at");

CREATE INDEX IF NOT EXISTS "idx_auth_login_event_user_time"
    ON "auth_login_event" ("tenant_id", "user_id", "occurred_at");

CREATE INDEX IF NOT EXISTS "idx_auth_login_event_identifier"
    ON "auth_login_event" ("tenant_id", "identifier_hash", "occurred_at");

COMMENT ON TABLE "auth_login_event" IS '认证结果审计事件；每个认证事务终态一行，含不存在账号的失败尝试';
COMMENT ON COLUMN "auth_login_event"."identifier_hash" IS '规范化标识的 SHA-256 十六进制，不保存原始值';
COMMENT ON COLUMN "auth_login_event"."transaction_id" IS '来源认证事务；唯一约束保证同一事务只投喂一次';
COMMENT ON COLUMN "auth_login_event"."amr" IS '认证方法引用 CSV，与会话记录一致';
