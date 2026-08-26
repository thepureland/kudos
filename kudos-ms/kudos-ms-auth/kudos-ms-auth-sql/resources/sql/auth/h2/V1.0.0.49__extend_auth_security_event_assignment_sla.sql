ALTER TABLE "auth_security_event" ADD COLUMN IF NOT EXISTS "assigned_to" VARCHAR(36);
ALTER TABLE "auth_security_event" ADD COLUMN IF NOT EXISTS "assigned_by" VARCHAR(36);
ALTER TABLE "auth_security_event" ADD COLUMN IF NOT EXISTS "assigned_at" TIMESTAMP;
ALTER TABLE "auth_security_event" ADD COLUMN IF NOT EXISTS "assignment_reason" VARCHAR(512);
ALTER TABLE "auth_security_event" ADD COLUMN IF NOT EXISTS "due_at" TIMESTAMP;

ALTER TABLE "auth_security_event" ADD CONSTRAINT IF NOT EXISTS "ck_auth_security_event_assignment_fields"
    CHECK (
        (
            "assigned_to" IS NULL AND "assigned_by" IS NULL
            AND "assigned_at" IS NULL AND "assignment_reason" IS NULL
        ) OR (
            "assigned_to" IS NOT NULL AND "assigned_by" IS NOT NULL
            AND "assigned_at" IS NOT NULL AND "assignment_reason" IS NOT NULL
        )
    );

CREATE INDEX IF NOT EXISTS "idx_auth_security_event_tenant_assignee_status"
    ON "auth_security_event" ("tenant_id", "assigned_to", "status", "last_occurred_at");

CREATE INDEX IF NOT EXISTS "idx_auth_security_event_tenant_status_due"
    ON "auth_security_event" ("tenant_id", "status", "due_at");
