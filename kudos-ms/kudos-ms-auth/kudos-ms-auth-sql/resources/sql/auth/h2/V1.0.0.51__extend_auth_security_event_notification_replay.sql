ALTER TABLE "auth_security_event_notification" ADD COLUMN IF NOT EXISTS "replay_count" INT NOT NULL DEFAULT 0;

ALTER TABLE "auth_security_event_notification" ADD CONSTRAINT IF NOT EXISTS
    "ck_auth_security_event_notification_replay_count" CHECK ("replay_count" >= 0);

CREATE INDEX IF NOT EXISTS "idx_auth_security_event_notification_dead"
    ON "auth_security_event_notification" ("tenant_id", "status", "update_time");

CREATE TABLE IF NOT EXISTS "auth_security_event_notification_replay" (
    "id" VARCHAR(36) NOT NULL,
    "tenant_id" VARCHAR(36) NOT NULL,
    "notification_id" VARCHAR(36) NOT NULL,
    "actor_user_id" VARCHAR(36) NOT NULL,
    "reason" VARCHAR(512) NOT NULL,
    "replayed_at" TIMESTAMP NOT NULL,
    CONSTRAINT "pk_auth_security_event_notification_replay" PRIMARY KEY ("id"),
    CONSTRAINT "fk_auth_security_event_notification_replay_notification" FOREIGN KEY ("notification_id")
        REFERENCES "auth_security_event_notification" ("id"),
    CONSTRAINT "ck_auth_security_event_notification_replay_reason" CHECK (CHAR_LENGTH("reason") > 0)
);

CREATE INDEX IF NOT EXISTS "idx_auth_security_event_notification_replay_notification"
    ON "auth_security_event_notification_replay" ("tenant_id", "notification_id", "replayed_at");
