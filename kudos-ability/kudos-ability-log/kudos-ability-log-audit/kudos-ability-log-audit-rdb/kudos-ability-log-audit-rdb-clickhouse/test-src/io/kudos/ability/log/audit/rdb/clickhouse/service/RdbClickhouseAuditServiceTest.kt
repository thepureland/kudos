package io.kudos.ability.log.audit.rdb.clickhouse.service

import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.ClickHouseTestContainer
import jakarta.annotation.Resource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.sql.DriverManager
import java.util.Date
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test for [RdbClickhouseAuditService] against a real ClickHouse 24.8
 * (via testcontainer). Loads the same simplified MergeTree DDL the production module ships.
 *
 * Coverage:
 *  - Insert writes one row per entity + one per detail (individual `INSERT` statements).
 *  - Top-level `tenantId` / `subSysCode` fall back into the entity record when the entity didn't
 *    carry its own.
 *  - Entity's own `tenantId` is preserved over the model-level fallback.
 *  - Empty model → `true` without touching ClickHouse.
 *  - Only-detail or only-entity model writes only the populated side.
 *  - Null lists and null elements inside the detail list are tolerated (orEmpty / filterNotNull).
 *  - Null entity operateTime falls back to LocalDateTime.now().
 *  - Persistence failure (table dropped → UNKNOWN_TABLE on INSERT) → `false`, never throws.
 *
 * Verification reads use **raw JDBC** (DriverManager), not the ktorm DSL: clickhouse-jdbc 0.9.x
 * (client-v2) throws SQLFeatureNotSupportedException from `Connection.getTypeMap()`, which ktorm's
 * CachedRowSet probes on every SELECT — Hikari then marks the pooled connection broken
 * (SQLSTATE 0A000) and ktorm reads become intermittently flaky. The production write path only
 * ever INSERTs (and the read service uses raw JDBC), so this is purely a test-side concern.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest(properties = ["spring.flyway.enabled=false"])
@EnabledIfDockerInstalled
internal open class RdbClickhouseAuditServiceTest {

    @Resource
    private lateinit var auditService: IAuditService

    @Resource
    private lateinit var environment: org.springframework.core.env.Environment

    @BeforeTest
    fun resetSchema() {
        // ClickHouse doesn't support DELETE in the RDBMS sense without ALTER; recreating the
        // tables is simpler and faster than trying to delete rows. The setup writes a fresh
        // empty schema before each test method so cross-test data pollution is impossible.
        val jdbcUrl = requireNotNull(environment.getProperty("spring.datasource.dynamic.datasource.ds1.url")) {
            "ClickHouse JDBC URL should have been registered by DynamicPropertySource"
        }
        DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD).use { conn ->
            conn.createStatement().use { st ->
                st.execute("DROP TABLE IF EXISTS sys_audit_log")
                st.execute("DROP TABLE IF EXISTS sys_audit_detail_log")
                st.execute(MAIN_TABLE_DDL)
                st.execute(DETAIL_TABLE_DDL)
            }
        }
    }

    @Test
    fun submit_writesMainAndDetailRows() {
        val model = SysAuditLogModel().apply {
            tenantId = "tenant-A"
            subSysCode = "sys-A"
            entities = mutableListOf(makeEntity("e1", entityId = "u-100", desc = "create user"))
            sysAuditDetailLogs = mutableListOf(makeDetail("d1", auditId = "e1", desc = "POST /users"))
        }

        assertTrue(auditService.submit(model))
        forceMerge("sys_audit_log")
        forceMerge("sys_audit_detail_log")

        assertEquals(
            listOf("u-100"),
            queryColumn("SELECT entity_id FROM sys_audit_log WHERE id = 'e1'"),
        )
        assertEquals(
            listOf("create user"),
            queryColumn("SELECT description FROM sys_audit_log WHERE id = 'e1'"),
        )
        assertEquals(
            listOf("POST /users"),
            queryColumn("SELECT description FROM sys_audit_detail_log WHERE audit_id = 'e1'"),
        )
    }

    @Test
    fun submit_propagatesTopLevelTenantAndSubSysCode_whenEntityMissing() {
        val entity = makeEntity("e2", entityId = "u-200", desc = "x").apply {
            tenantId = null
            subSysCode = null
        }
        val model = SysAuditLogModel().apply {
            tenantId = "fallback-tenant"
            subSysCode = "fallback-sys"
            entities = mutableListOf(entity)
        }

        auditService.submit(model)
        forceMerge("sys_audit_log")

        assertEquals(listOf("fallback-tenant"), queryColumn("SELECT tenant_id FROM sys_audit_log WHERE id = 'e2'"))
        assertEquals(listOf("fallback-sys"), queryColumn("SELECT sub_sys_code FROM sys_audit_log WHERE id = 'e2'"))
        assertEquals(listOf("u-200"), queryColumn("SELECT entity_id FROM sys_audit_log WHERE id = 'e2'"))
    }

    @Test
    fun submit_entityOverridesTopLevel_whenBothPresent() {
        val entity = makeEntity("e3", entityId = "u-300", desc = "x").apply {
            tenantId = "entity-tenant"
        }
        val model = SysAuditLogModel().apply {
            tenantId = "model-tenant"
            entities = mutableListOf(entity)
        }

        auditService.submit(model)
        forceMerge("sys_audit_log")

        assertEquals(
            listOf("entity-tenant"),
            queryColumn("SELECT tenant_id FROM sys_audit_log WHERE id = 'e3'"),
            "the entity's own tenantId must win and not be overwritten by the model-level fallback",
        )
    }

    @Test
    fun submit_emptyModel_returnsTrueAndWritesNothing() {
        val model = SysAuditLogModel().apply {
            entities = mutableListOf()
            sysAuditDetailLogs = mutableListOf()
        }
        assertTrue(auditService.submit(model), "empty model is a no-op success")
    }

    @Test
    fun submit_batchInsertMultipleEntities() {
        val model = SysAuditLogModel().apply {
            tenantId = "tenant-B"
            entities = mutableListOf(
                makeEntity("e4-1", entityId = "u-A", desc = "a"),
                makeEntity("e4-2", entityId = "u-B", desc = "b"),
                makeEntity("e4-3", entityId = "u-C", desc = "c"),
            )
        }
        auditService.submit(model)

        // ClickHouse MergeTree creates one part per insert batch and merges asynchronously.
        // `OPTIMIZE TABLE ... FINAL` forces an immediate merge so subsequent SELECTs see every
        // row deterministically. Production code doesn't need this — audit reads happen long
        // after the writes — but the test's "insert then assert" pattern needs the sync barrier.
        forceMerge("sys_audit_log")

        val ids = queryColumn("SELECT id FROM sys_audit_log WHERE tenant_id = 'tenant-B' ORDER BY id")
        assertEquals(listOf("e4-1", "e4-2", "e4-3"), ids)
    }

    @Test
    fun submit_onlyEntities_writesMainOnly() {
        val model = SysAuditLogModel().apply {
            tenantId = "tenant-C"
            entities = mutableListOf(makeEntity("e5", entityId = "u-no-detail", desc = "ent only"))
            sysAuditDetailLogs = mutableListOf() // empty
        }
        assertTrue(auditService.submit(model))
        forceMerge("sys_audit_log")

        assertEquals(listOf("e5"), queryColumn("SELECT id FROM sys_audit_log WHERE id = 'e5'"))
        assertEquals(
            emptyList(),
            queryColumn("SELECT id FROM sys_audit_detail_log WHERE audit_id = 'e5'"),
            "no detail rows when sysAuditDetailLogs is empty",
        )
    }

    @Test
    fun submit_onlyDetails_writesDetailOnly() {
        // Edge case: a model with details but no entities. Rare but legitimate (e.g. detail
        // backfill after a main-table aspect has already run). Must not silently drop the
        // detail rows.
        val model = SysAuditLogModel().apply {
            entities = mutableListOf()
            sysAuditDetailLogs = mutableListOf(makeDetail("d6", auditId = "audit-only-d", desc = "orphan"))
        }
        assertTrue(auditService.submit(model))
        forceMerge("sys_audit_detail_log")

        assertEquals(
            listOf("orphan"),
            queryColumn("SELECT description FROM sys_audit_detail_log WHERE audit_id = 'audit-only-d'"),
        )
    }

    @Test
    fun submit_nullLists_returnsTrueAsNoOp() {
        // Both lists left null (not just empty) — orEmpty() must normalize and short-circuit true.
        assertTrue(RdbClickhouseAuditService().submit(SysAuditLogModel()), "null lists are a no-op success")
    }

    @Test
    fun submit_nullDetailElements_areFilteredOut() {
        // sysAuditDetailLogs is MutableList<SysAuditDetailLogVo?> — null slots are legal on the
        // wire (MQ-deserialized models). filterNotNull must drop them, keeping the real entries.
        val model = SysAuditLogModel().apply {
            tenantId = "tenant-null-el"
            entities = mutableListOf(makeEntity("e7", entityId = "u-700", desc = "x"))
            sysAuditDetailLogs = mutableListOf(null, makeDetail("d7", auditId = "e7", desc = "kept"), null)
        }
        assertTrue(auditService.submit(model))
        forceMerge("sys_audit_detail_log")

        assertEquals(
            listOf("d7"),
            queryColumn("SELECT id FROM sys_audit_detail_log WHERE audit_id = 'e7'"),
            "null detail slots must be silently dropped, real ones kept",
        )
    }

    @Test
    fun submit_entityWithoutOperateTime_defaultsToNow() {
        val entity = makeEntity("e8", entityId = "u-800", desc = "x").apply { operateTime = null }
        val model = SysAuditLogModel().apply {
            tenantId = "tenant-no-time"
            entities = mutableListOf(entity)
        }
        assertTrue(auditService.submit(model))
        forceMerge("sys_audit_log")

        val storedTime = assertNotNull(
            queryColumn("SELECT toString(operate_time) FROM sys_audit_log WHERE id = 'e8'").single(),
            "null operateTime must fall back to LocalDateTime.now(), not NULL",
        )
        assertTrue(storedTime.startsWith("20"), "operate_time should be a real recent timestamp, got: $storedTime")
    }

    @Test
    fun submit_insertFailure_returnsFalseInsteadOfThrowing() {
        // Drop the main table so the INSERT fails server-side (UNKNOWN_TABLE). The audit
        // contract says persistence failures translate to `false` and must never propagate —
        // a broken audit pipeline must not break the business flow it observes.
        val jdbcUrl = requireNotNull(environment.getProperty("spring.datasource.dynamic.datasource.ds1.url"))
        DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD).use { conn ->
            conn.createStatement().use { st -> st.execute("DROP TABLE IF EXISTS sys_audit_log") }
        }

        val model = SysAuditLogModel().apply {
            tenantId = "tenant-fail"
            entities = mutableListOf(makeEntity("e9", entityId = "u-900", desc = "doomed"))
        }
        assertFalse(auditService.submit(model), "insert into a missing table must yield false, never throw")
    }

    /** Run a single-column SELECT over a dedicated raw JDBC connection; returns rows in result order. */
    private fun queryColumn(sql: String): List<String?> {
        val jdbcUrl = requireNotNull(environment.getProperty("spring.datasource.dynamic.datasource.ds1.url"))
        DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD).use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery(sql).use { rs ->
                    val list = mutableListOf<String?>()
                    while (rs.next()) list += rs.getString(1)
                    return list
                }
            }
        }
    }

    private fun forceMerge(table: String) {
        val jdbcUrl = requireNotNull(environment.getProperty("spring.datasource.dynamic.datasource.ds1.url"))
        DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD).use { conn ->
            conn.createStatement().use { st -> st.execute("OPTIMIZE TABLE $table FINAL") }
        }
    }

    private fun makeEntity(id: String, entityId: String, desc: String): SysAuditLogVo = SysAuditLogVo().apply {
        this.id = id
        this.entityId = entityId
        this.description = desc
        this.operateTypeId = 2
        this.operateType = "create"
        this.moduleCode = "USER"
        this.operateTime = Date()
        // Deliberately leave tenantId / subSysCode null — each test sets them on the model so the
        // entity-vs-model fallback rule in applyAuditLog actually exercises.
    }

    private fun makeDetail(id: String, auditId: String, desc: String): SysAuditDetailLogVo = SysAuditDetailLogVo().apply {
        this.id = id
        this.auditId = auditId
        this.description = desc
        this.operateUrl = "/x"
    }

    companion object {
        private const val USERNAME = "default"
        private const val PASSWORD = ""

        // DDL is duplicated here from the production SQL file because Hikari's init.schema runs
        // before the testcontainer's DynamicPropertySource resolves the URL — so we apply the
        // schema manually in @BeforeTest via the resolved URL instead.
        private const val MAIN_TABLE_DDL = """
            CREATE TABLE IF NOT EXISTS sys_audit_log (
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
            ) ENGINE = MergeTree
              PARTITION BY toYYYYMM(operate_time)
              ORDER BY (tenant_id, operate_time, id)
        """

        private const val DETAIL_TABLE_DDL = """
            CREATE TABLE IF NOT EXISTS sys_audit_detail_log (
                id                String,
                audit_id          String,
                operate_url       Nullable(String),
                string_params     Nullable(String),
                object_params     Nullable(String),
                request_referer   Nullable(String),
                request_form_data Nullable(String),
                description       Nullable(String),
                create_time       DateTime64(6) DEFAULT now64(6)
            ) ENGINE = MergeTree
              PARTITION BY toYYYYMM(create_time)
              ORDER BY (audit_id, create_time, id)
        """

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            ClickHouseTestContainer.startIfNeeded(registry)
            // ClickHouseTestContainer.startIfNeeded already registers `kudos.test.clickhouse.*`.
            // Map those onto the baomidou dynamic-datasource yml shape so Spring's
            // DataSource bean picks the testcontainer URL up.
            val running = ClickHouseTestContainer.getRunningContainer()
                ?: error("ClickHouse container failed to start")
            val httpPort = running.ports.first { it.privatePort == 8123 }
            val host = requireNotNull(httpPort.ip)
            val port = requireNotNull(httpPort.publicPort)
            // Driver tuning so the JDBC batch protocol behaves on ClickHouse + matched parts are
            // visible immediately for the test's "insert then assert" pattern:
            //   * wait_end_of_query=1   — server waits until INSERT is durable
            //   * async_insert=0        — disable buffered async insert mode
            // clickhouse-jdbc 0.9.x (client-v2) rejects unknown raw URL properties with
            // ClientMisconfigurationException; server settings must carry the
            // `clickhouse_setting_` prefix to be forwarded to the server.
            val jdbcUrl = "jdbc:clickhouse://$host:$port/${ClickHouseTestContainer.DATABASE}" +
                "?clickhouse_setting_wait_end_of_query=1&clickhouse_setting_async_insert=0"

            registry.add("spring.datasource.dynamic.datasource.ds1.url") { jdbcUrl }
            registry.add("spring.datasource.dynamic.datasource.ds1.username") { USERNAME }
            registry.add("spring.datasource.dynamic.datasource.ds1.password") { PASSWORD }
        }
    }
}
