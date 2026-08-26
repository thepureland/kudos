ALTER TABLE "auth_security_event" ADD COLUMN IF NOT EXISTS "escalation_level" INT NOT NULL DEFAULT 0;
ALTER TABLE "auth_security_event" ADD COLUMN IF NOT EXISTS "last_escalated_at" TIMESTAMP;
ALTER TABLE "auth_security_event" ADD COLUMN IF NOT EXISTS "next_escalation_at" TIMESTAMP;

UPDATE "auth_security_event"
SET "next_escalation_at" = "due_at"
WHERE "escalation_level" = 0 AND "next_escalation_at" IS NULL AND "due_at" IS NOT NULL;

ALTER TABLE "auth_security_event" ADD CONSTRAINT IF NOT EXISTS "ck_auth_security_event_escalation_level"
    CHECK ("escalation_level" BETWEEN 0 AND 10);
ALTER TABLE "auth_security_event" ADD CONSTRAINT IF NOT EXISTS "ck_auth_security_event_escalation_fields"
    CHECK (
        ("escalation_level" = 0 AND "last_escalated_at" IS NULL)
        OR ("escalation_level" > 0 AND "last_escalated_at" IS NOT NULL)
    );

CREATE INDEX IF NOT EXISTS "idx_auth_security_event_escalation_due"
    ON "auth_security_event" ("status", "next_escalation_at");

CREATE TABLE IF NOT EXISTS "auth_security_event_notification" (
    "id" VARCHAR(36) NOT NULL,
    "tenant_id" VARCHAR(36) NOT NULL,
    "event_id" VARCHAR(36) NOT NULL,
    "notification_type" VARCHAR(32) NOT NULL,
    "escalation_level" INT NOT NULL,
    "recipient_user_id" VARCHAR(36),
    "due_at" TIMESTAMP,
    "escalated_at" TIMESTAMP NOT NULL,
    "status" VARCHAR(16) NOT NULL,
    "attempt_count" INT NOT NULL DEFAULT 0,
    "next_attempt_at" TIMESTAMP,
    "lease_owner" VARCHAR(64),
    "lease_until" TIMESTAMP,
    "delivered_at" TIMESTAMP,
    "last_error_code" VARCHAR(64),
    "create_time" TIMESTAMP NOT NULL,
    "update_time" TIMESTAMP NOT NULL,
    CONSTRAINT "pk_auth_security_event_notification" PRIMARY KEY ("id"),
    CONSTRAINT "fk_auth_security_event_notification_event" FOREIGN KEY ("event_id")
        REFERENCES "auth_security_event" ("id"),
    CONSTRAINT "uk_auth_security_event_notification_level" UNIQUE
        ("event_id", "notification_type", "escalation_level"),
    CONSTRAINT "ck_auth_security_event_notification_type" CHECK
        ("notification_type" IN ('SLA_ESCALATED')),
    CONSTRAINT "ck_auth_security_event_notification_level" CHECK
        ("escalation_level" BETWEEN 1 AND 10),
    CONSTRAINT "ck_auth_security_event_notification_attempt" CHECK ("attempt_count" >= 0),
    CONSTRAINT "ck_auth_security_event_notification_status" CHECK
        ("status" IN ('PENDING', 'PROCESSING', 'DELIVERED', 'DEAD')),
    CONSTRAINT "ck_auth_security_event_notification_delivery_fields" CHECK (
        ("status" = 'PENDING' AND "next_attempt_at" IS NOT NULL
            AND "lease_owner" IS NULL AND "lease_until" IS NULL AND "delivered_at" IS NULL)
        OR ("status" = 'PROCESSING' AND "next_attempt_at" IS NOT NULL
            AND "lease_owner" IS NOT NULL AND "lease_until" IS NOT NULL AND "delivered_at" IS NULL)
        OR ("status" = 'DELIVERED' AND "next_attempt_at" IS NULL
            AND "lease_owner" IS NULL AND "lease_until" IS NULL AND "delivered_at" IS NOT NULL)
        OR ("status" = 'DEAD' AND "next_attempt_at" IS NULL
            AND "lease_owner" IS NULL AND "lease_until" IS NULL AND "delivered_at" IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS "idx_auth_security_event_notification_claim"
    ON "auth_security_event_notification" ("status", "next_attempt_at", "lease_until");

CREATE INDEX IF NOT EXISTS "idx_auth_security_event_notification_tenant_event"
    ON "auth_security_event_notification" ("tenant_id", "event_id");
