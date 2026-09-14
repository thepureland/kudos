ALTER TABLE "auth_security_event" ADD COLUMN IF NOT EXISTS "workflow_version" BIGINT NOT NULL DEFAULT 0;
ALTER TABLE "auth_security_event" ADD COLUMN IF NOT EXISTS "acknowledged_by" VARCHAR(36);
ALTER TABLE "auth_security_event" ADD COLUMN IF NOT EXISTS "acknowledged_at" TIMESTAMP;
ALTER TABLE "auth_security_event" ADD COLUMN IF NOT EXISTS "acknowledge_reason" VARCHAR(512);
ALTER TABLE "auth_security_event" ADD COLUMN IF NOT EXISTS "resolution" VARCHAR(32);
ALTER TABLE "auth_security_event" ADD COLUMN IF NOT EXISTS "closed_by" VARCHAR(36);
ALTER TABLE "auth_security_event" ADD COLUMN IF NOT EXISTS "closed_at" TIMESTAMP;
ALTER TABLE "auth_security_event" ADD COLUMN IF NOT EXISTS "close_reason" VARCHAR(512);

ALTER TABLE "auth_security_event" ADD CONSTRAINT "ck_auth_security_event_workflow_version"
    CHECK ("workflow_version" >= 0);

ALTER TABLE "auth_security_event" ADD CONSTRAINT "ck_auth_security_event_status"
    CHECK ("status" IN ('OPEN', 'ACKNOWLEDGED', 'CLOSED'));

ALTER TABLE "auth_security_event" ADD CONSTRAINT "ck_auth_security_event_workflow_fields"
    CHECK (
        (
            "status" = 'OPEN'
            AND "acknowledged_by" IS NULL AND "acknowledged_at" IS NULL AND "acknowledge_reason" IS NULL
            AND "resolution" IS NULL AND "closed_by" IS NULL AND "closed_at" IS NULL AND "close_reason" IS NULL
        ) OR (
            "status" = 'ACKNOWLEDGED'
            AND "acknowledged_by" IS NOT NULL AND "acknowledged_at" IS NOT NULL
            AND "acknowledge_reason" IS NOT NULL
            AND "resolution" IS NULL AND "closed_by" IS NULL AND "closed_at" IS NULL AND "close_reason" IS NULL
        ) OR (
            "status" = 'CLOSED'
            AND "acknowledged_by" IS NOT NULL AND "acknowledged_at" IS NOT NULL
            AND "acknowledge_reason" IS NOT NULL
            AND "resolution" IS NOT NULL AND "closed_by" IS NOT NULL
            AND "closed_at" IS NOT NULL AND "close_reason" IS NOT NULL
            AND "acknowledged_at" <= "closed_at"
        )
    );
