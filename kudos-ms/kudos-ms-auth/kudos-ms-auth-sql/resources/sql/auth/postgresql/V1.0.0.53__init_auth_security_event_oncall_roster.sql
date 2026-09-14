--region DDL
CREATE TABLE IF NOT EXISTS "auth_security_event_oncall_roster" (
    "id" VARCHAR(36) NOT NULL,
    "tenant_id" VARCHAR(36) NOT NULL,
    "roster_code" VARCHAR(64) NOT NULL,
    "display_name" VARCHAR(128) NOT NULL,
    "enabled" BOOLEAN NOT NULL,
    "config_version" BIGINT NOT NULL DEFAULT 0,
    "create_user_id" VARCHAR(36) NOT NULL,
    "create_reason" VARCHAR(512) NOT NULL,
    "create_time" TIMESTAMP NOT NULL,
    "update_user_id" VARCHAR(36) NOT NULL,
    "update_reason" VARCHAR(512) NOT NULL,
    "update_time" TIMESTAMP NOT NULL,
    CONSTRAINT "pk_auth_security_event_oncall_roster" PRIMARY KEY ("id"),
    CONSTRAINT "uk_auth_security_event_oncall_roster_code" UNIQUE ("tenant_id", "roster_code"),
    CONSTRAINT "ck_auth_security_event_oncall_roster_code" CHECK (CHAR_LENGTH("roster_code") > 0),
    CONSTRAINT "ck_auth_security_event_oncall_roster_name" CHECK (CHAR_LENGTH("display_name") > 0),
    CONSTRAINT "ck_auth_security_event_oncall_roster_version" CHECK ("config_version" >= 0)
);

CREATE TABLE IF NOT EXISTS "auth_security_event_oncall_shift" (
    "id" VARCHAR(36) NOT NULL,
    "tenant_id" VARCHAR(36) NOT NULL,
    "roster_id" VARCHAR(36) NOT NULL,
    "responder_user_id" VARCHAR(36) NOT NULL,
    "tier" INT NOT NULL,
    "start_at" TIMESTAMP NOT NULL,
    "end_at" TIMESTAMP NOT NULL,
    CONSTRAINT "pk_auth_security_event_oncall_shift" PRIMARY KEY ("id"),
    CONSTRAINT "fk_auth_security_event_oncall_shift_roster" FOREIGN KEY ("roster_id")
        REFERENCES "auth_security_event_oncall_roster" ("id"),
    CONSTRAINT "ck_auth_security_event_oncall_shift_tier" CHECK ("tier" BETWEEN 1 AND 5),
    CONSTRAINT "ck_auth_security_event_oncall_shift_window" CHECK ("start_at" < "end_at")
);

CREATE INDEX IF NOT EXISTS "idx_auth_security_event_oncall_shift_roster"
    ON "auth_security_event_oncall_shift" ("tenant_id", "roster_id", "start_at", "end_at");

CREATE TABLE IF NOT EXISTS "auth_security_event_oncall_roster_audit" (
    "id" VARCHAR(36) NOT NULL,
    "tenant_id" VARCHAR(36) NOT NULL,
    "roster_id" VARCHAR(36) NOT NULL,
    "actor_user_id" VARCHAR(36) NOT NULL,
    "reason" VARCHAR(512) NOT NULL,
    "config_version" BIGINT NOT NULL,
    "before_snapshot" VARCHAR(8192),
    "after_snapshot" VARCHAR(8192) NOT NULL,
    "changed_at" TIMESTAMP NOT NULL,
    CONSTRAINT "pk_auth_security_event_oncall_roster_audit" PRIMARY KEY ("id"),
    CONSTRAINT "fk_auth_security_event_oncall_roster_audit_roster" FOREIGN KEY ("roster_id")
        REFERENCES "auth_security_event_oncall_roster" ("id"),
    CONSTRAINT "ck_auth_security_event_oncall_roster_audit_reason" CHECK (CHAR_LENGTH("reason") > 0),
    CONSTRAINT "ck_auth_security_event_oncall_roster_audit_version" CHECK ("config_version" > 0)
);

CREATE INDEX IF NOT EXISTS "idx_auth_security_event_oncall_roster_audit_roster"
    ON "auth_security_event_oncall_roster_audit" ("tenant_id", "roster_id", "changed_at");

ALTER TABLE "auth_security_event_notification_route"
    ADD COLUMN IF NOT EXISTS "responder_roster_code" VARCHAR(64);

COMMENT ON TABLE "auth_security_event_oncall_roster" IS '安全事件响应值班表，按租户和值班表编码唯一';
COMMENT ON TABLE "auth_security_event_oncall_shift" IS '值班班次；显式 UTC 时间窗，tier 越小越优先';
COMMENT ON TABLE "auth_security_event_oncall_roster_audit" IS '值班表变更的追加式审计，保留变更前后快照';
COMMENT ON COLUMN "auth_security_event_oncall_shift"."tier" IS '值班层级；升级级别 N 命中 tier <= N 的班次';
COMMENT ON COLUMN "auth_security_event_notification_route"."responder_roster_code" IS '可选值班表编码，解析结果并入收件人';
--endregion DDL


--region DML
INSERT INTO "sys_cache" ("name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time", "ttl", "remark", "built_in", "hash") VALUES
    ('AUTH_SECURITY_EVENT_ONCALL_ROSTER_BY_TENANT_ID', 'auth', 'LOCAL_REMOTE', false, true, 999999999, '安全事件值班表缓存(by tenantId)', true, false);
--endregion DML
