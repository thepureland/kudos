--region DDL
CREATE TABLE IF NOT EXISTS "auth_security_event_notification_route" (
    "id" VARCHAR(36) NOT NULL,
    "tenant_id" VARCHAR(36) NOT NULL,
    "notification_type" VARCHAR(32) NOT NULL,
    "applies_to" VARCHAR(16) NOT NULL,
    "route_code" VARCHAR(64) NOT NULL,
    "destination" VARCHAR(32) NOT NULL,
    "channels" VARCHAR(256) NOT NULL,
    "responder_user_ids" VARCHAR(2048),
    "include_assignee" BOOLEAN NOT NULL,
    "enabled" BOOLEAN NOT NULL,
    "fallback_behavior" VARCHAR(32) NOT NULL,
    "config_version" BIGINT NOT NULL DEFAULT 0,
    "create_user_id" VARCHAR(36) NOT NULL,
    "create_reason" VARCHAR(512) NOT NULL,
    "create_time" TIMESTAMP NOT NULL,
    "update_user_id" VARCHAR(36) NOT NULL,
    "update_reason" VARCHAR(512) NOT NULL,
    "update_time" TIMESTAMP NOT NULL,
    CONSTRAINT "pk_auth_security_event_notification_route" PRIMARY KEY ("id"),
    CONSTRAINT "uk_auth_security_event_notification_route_scope" UNIQUE
        ("tenant_id", "notification_type", "applies_to"),
    CONSTRAINT "ck_auth_security_event_notification_route_type" CHECK
        ("notification_type" IN ('SLA_ESCALATED')),
    CONSTRAINT "ck_auth_security_event_notification_route_applies_to" CHECK
        ("applies_to" IN ('ASSIGNED', 'UNASSIGNED')),
    CONSTRAINT "ck_auth_security_event_notification_route_destination" CHECK
        ("destination" IN ('USER', 'TENANT_SECURITY_QUEUE')),
    CONSTRAINT "ck_auth_security_event_notification_route_fallback" CHECK
        ("fallback_behavior" IN ('DEFAULT_ROUTE', 'TENANT_SECURITY_QUEUE', 'FAIL')),
    CONSTRAINT "ck_auth_security_event_notification_route_code" CHECK (CHAR_LENGTH("route_code") > 0),
    CONSTRAINT "ck_auth_security_event_notification_route_channels" CHECK (CHAR_LENGTH("channels") > 0),
    CONSTRAINT "ck_auth_security_event_notification_route_version" CHECK ("config_version" >= 0)
);

CREATE INDEX IF NOT EXISTS "idx_auth_security_event_notification_route_tenant"
    ON "auth_security_event_notification_route" ("tenant_id", "notification_type");

COMMENT ON TABLE "auth_security_event_notification_route" IS '认证安全事件通知的租户级路由配置';
COMMENT ON COLUMN "auth_security_event_notification_route"."applies_to" IS '适用的投递形态：已分派/未分派';
COMMENT ON COLUMN "auth_security_event_notification_route"."channels" IS '渠道枚举 CSV，非空';
COMMENT ON COLUMN "auth_security_event_notification_route"."responder_user_ids" IS '固定响应人用户ID CSV，可空';
COMMENT ON COLUMN "auth_security_event_notification_route"."include_assignee" IS '是否把 outbox 中的负责人快照并入收件人';
COMMENT ON COLUMN "auth_security_event_notification_route"."fallback_behavior" IS '配置无法产生可投递收件人时的降级行为';
COMMENT ON COLUMN "auth_security_event_notification_route"."config_version" IS '乐观并发版本，每次保存 +1';

CREATE TABLE IF NOT EXISTS "auth_security_event_notification_route_audit" (
    "id" VARCHAR(36) NOT NULL,
    "tenant_id" VARCHAR(36) NOT NULL,
    "route_id" VARCHAR(36) NOT NULL,
    "actor_user_id" VARCHAR(36) NOT NULL,
    "reason" VARCHAR(512) NOT NULL,
    "config_version" BIGINT NOT NULL,
    "before_snapshot" VARCHAR(4096),
    "after_snapshot" VARCHAR(4096) NOT NULL,
    "changed_at" TIMESTAMP NOT NULL,
    CONSTRAINT "pk_auth_security_event_notification_route_audit" PRIMARY KEY ("id"),
    CONSTRAINT "fk_auth_security_event_notification_route_audit_route" FOREIGN KEY ("route_id")
        REFERENCES "auth_security_event_notification_route" ("id"),
    CONSTRAINT "ck_auth_security_event_notification_route_audit_reason" CHECK (CHAR_LENGTH("reason") > 0),
    CONSTRAINT "ck_auth_security_event_notification_route_audit_version" CHECK ("config_version" > 0)
);

CREATE INDEX IF NOT EXISTS "idx_auth_security_event_notification_route_audit_route"
    ON "auth_security_event_notification_route_audit" ("tenant_id", "route_id", "changed_at");

COMMENT ON TABLE "auth_security_event_notification_route_audit" IS '通知路由配置的追加式变更审计，保留变更前后快照';
--endregion DDL


--region DML
INSERT INTO "sys_cache" ("name", "atomic_service_code", "strategy_dict_code", "write_on_boot", "write_in_time", "ttl", "remark", "built_in", "hash") VALUES
    ('AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_BY_TENANT_ID', 'auth', 'LOCAL_REMOTE', false, true, 999999999, '安全事件通知路由配置缓存(by tenantId)', true, false);
--endregion DML
