CREATE TABLE IF NOT EXISTS "auth_security_event_notification_channel" (
    "id" VARCHAR(36) NOT NULL,
    "tenant_id" VARCHAR(36) NOT NULL,
    "notification_id" VARCHAR(36) NOT NULL,
    "channel" VARCHAR(32) NOT NULL,
    "status" VARCHAR(16) NOT NULL,
    "attempt_count" INT NOT NULL,
    "last_error_code" VARCHAR(64),
    "settled_at" TIMESTAMP NOT NULL,
    CONSTRAINT "pk_auth_security_event_notification_channel" PRIMARY KEY ("id"),
    CONSTRAINT "fk_auth_security_event_notification_channel_notification" FOREIGN KEY ("notification_id")
        REFERENCES "auth_security_event_notification" ("id"),
    -- The ledger's whole purpose: one settled outcome per channel, enforced by the database rather than by
    -- the dispatcher remembering not to write twice.
    CONSTRAINT "uk_auth_security_event_notification_channel" UNIQUE ("notification_id", "channel"),
    CONSTRAINT "ck_auth_security_event_notification_channel_name" CHECK
        ("channel" IN ('SITE_MESSAGE', 'EMAIL', 'SMS', 'IM', 'WEBHOOK', 'WORK_ORDER', 'EVENT_BUS')),
    -- Only terminal outcomes are recorded; a channel that failed retryably stays absent and is attempted again.
    CONSTRAINT "ck_auth_security_event_notification_channel_status" CHECK ("status" IN ('DELIVERED', 'DEAD')),
    CONSTRAINT "ck_auth_security_event_notification_channel_attempt" CHECK ("attempt_count" >= 0),
    CONSTRAINT "ck_auth_security_event_notification_channel_error" CHECK (
        ("status" = 'DELIVERED' AND "last_error_code" IS NULL)
        OR ("status" = 'DEAD' AND "last_error_code" IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS "idx_auth_security_event_notification_channel_tenant"
    ON "auth_security_event_notification_channel" ("tenant_id", "notification_id");

COMMENT ON TABLE "auth_security_event_notification_channel" IS '通知的逐渠道终态账本，使重试不再重发已交付渠道';
COMMENT ON COLUMN "auth_security_event_notification_channel"."status" IS '仅记录终态：DELIVERED 或永久失败的 DEAD';
COMMENT ON COLUMN "auth_security_event_notification_channel"."attempt_count" IS '结算时该通知行的尝试次数';
