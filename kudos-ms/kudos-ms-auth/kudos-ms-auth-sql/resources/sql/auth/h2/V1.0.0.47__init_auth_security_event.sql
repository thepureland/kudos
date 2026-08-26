CREATE TABLE IF NOT EXISTS "auth_security_event" (
    "id" VARCHAR(36) NOT NULL,
    "tenant_id" VARCHAR(36) NOT NULL,
    "user_id" VARCHAR(36) NOT NULL,
    "event_type" VARCHAR(64) NOT NULL,
    "subject_type" VARCHAR(32) NOT NULL,
    "subject_fingerprint" VARCHAR(128) NOT NULL,
    "risk_level" VARCHAR(32) NOT NULL,
    "risk_sources" CLOB,
    "risk_status_codes" CLOB,
    "deduplication_key" VARCHAR(43) NOT NULL,
    "bucket_start" TIMESTAMP NOT NULL,
    "status" VARCHAR(16) NOT NULL,
    "occurrence_count" BIGINT NOT NULL DEFAULT 1,
    "first_occurred_at" TIMESTAMP NOT NULL,
    "last_occurred_at" TIMESTAMP NOT NULL,
    "create_time" TIMESTAMP NOT NULL,
    "update_time" TIMESTAMP NOT NULL,
    CONSTRAINT "pk_auth_security_event" PRIMARY KEY ("id"),
    CONSTRAINT "uk_auth_security_event_dedup" UNIQUE (
        "tenant_id", "event_type", "deduplication_key", "bucket_start"
    ),
    CONSTRAINT "ck_auth_security_event_occurrence_count" CHECK ("occurrence_count" > 0),
    CONSTRAINT "ck_auth_security_event_time_order" CHECK ("first_occurred_at" <= "last_occurred_at")
);

CREATE INDEX IF NOT EXISTS "idx_auth_security_event_tenant_last"
    ON "auth_security_event" ("tenant_id", "last_occurred_at");

CREATE INDEX IF NOT EXISTS "idx_auth_security_event_tenant_user_last"
    ON "auth_security_event" ("tenant_id", "user_id", "last_occurred_at");
