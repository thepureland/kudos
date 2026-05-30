-- ClickHouse-flavored DDL for kudos audit-log tables.
--
-- Simplifications vs soul's production schema:
--   * No `ON CLUSTER` — single-node MergeTree. Multi-shard deployments add a `Distributed`
--     overlay table on top in their own DDL.
--   * No `ReplicatedMergeTree` — no ZooKeeper requirement. Apps that want replication run their
--     own DDL with the appropriate engine instead.
--   * No `Distributed` table / `View` over both. Those are deployment concerns, not module
--     concerns.
--   * No `TTL` clause. ClickHouse audit retention is a policy decision; the SQL ships
--     retention-free and downstream deployments add `MODIFY TTL` per their data-retention policy.
--
-- Column ordering of `ORDER BY (tenant_id, operate_time, id)` is the index sort key — chosen for
-- the most common admin query shape: "tenant X's audit between time A and B". Apps with different
-- query patterns should override the DDL.

CREATE TABLE IF NOT EXISTS sys_audit_log
(
    id                   String,
    entity_id            Nullable(String),
    operator_id          Nullable(String),
    operator             Nullable(String),
    operate_time         DateTime64(6),
    operate_type_id      Nullable(Int32),
    operate_type         LowCardinality(Nullable(String)),
    module_name          LowCardinality(Nullable(String)),
    module_code          LowCardinality(Nullable(String)),
    module_id            Nullable(Int32),
    description          Nullable(String),
    request_type         LowCardinality(Nullable(String)),
    client_os            LowCardinality(Nullable(String)),
    client_browser       LowCardinality(Nullable(String)),
    operator_user_type   LowCardinality(Nullable(String)),
    operate_ip           Nullable(Int64),
    operate_ip_dict_code Nullable(String),
    tenant_id            String,
    source_tenant_id     Nullable(String),
    sub_sys_code         LowCardinality(Nullable(String))
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(operate_time)
ORDER BY (tenant_id, operate_time, id);


CREATE TABLE IF NOT EXISTS sys_audit_detail_log
(
    id                String,
    audit_id          String,
    operate_url       Nullable(String),
    string_params     Nullable(String),
    object_params     Nullable(String),
    request_referer   Nullable(String),
    request_form_data Nullable(String),
    description       Nullable(String),
    create_time       DateTime64(6) DEFAULT now64(6)
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(create_time)
ORDER BY (audit_id, create_time, id);
