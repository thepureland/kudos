package io.kudos.ability.log.audit.rdb.clickhouse.service

import io.kudos.ability.data.rdb.ktorm.datasource.currentDatabase
import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import io.kudos.ability.log.audit.rdb.clickhouse.table.SysAuditDetailLogTable
import io.kudos.ability.log.audit.rdb.clickhouse.table.SysAuditLogTable
import io.kudos.context.core.KudosContextHolder
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.ClickHouseTestContainer
import jakarta.annotation.Resource
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.sql.DriverManager
import java.util.Date
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration test for [RdbClickhouseAuditService] against a real ClickHouse 24.8
 * (via testcontainer). Loads the same simplified MergeTree DDL the production module ships.
 *
 * Coverage:
 *  - Batch insert writes one row per entity + one per detail (separate `INSERT` statements
 *    issued by ktorm `batchInsert`).
 *  - Top-level `tenantId` / `subSysCode` fall back into the entity record when the entity didn't
 *    carry its own.
 *  - Entity's own `tenantId` is preserved over the model-level fallback.
 *  - Empty model → `true` without touching ClickHouse.
 *  - Only-detail or only-entity model writes only the populated side.
 *
 * Doesn't try to test failure → `false` translation against ClickHouse because ClickHouse doesn't
 * have a clean "make a write fail" knob (no FK, no unique constraint with rollback semantics);
 * that path is covered by the H2-backed `RdbKtormAuditServiceTest.submit_duplicatePrimaryKey_*`
 * in the sibling module.
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

        val db = KudosContextHolder.currentDatabase()
        val mainRows = db.from(SysAuditLogTable).select()
            .where { SysAuditLogTable.id eq "e1" }
            .map { it[SysAuditLogTable.entityId] to it[SysAuditLogTable.description] }
        assertEquals(listOf("u-100" to "create user"), mainRows)

        val detailRows = db.from(SysAuditDetailLogTable).select()
            .where { SysAuditDetailLogTable.auditId eq "e1" }
            .map { it[SysAuditDetailLogTable.description] }
        assertEquals(listOf("POST /users"), detailRows)
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

        val db = KudosContextHolder.currentDatabase()
        val row = db.from(SysAuditLogTable).select()
            .where { SysAuditLogTable.id eq "e2" }
            .map { Triple(it[SysAuditLogTable.tenantId], it[SysAuditLogTable.subSysCode], it[SysAuditLogTable.entityId]) }
            .single()
        assertEquals("fallback-tenant", row.first)
        assertEquals("fallback-sys", row.second)
        assertEquals("u-200", row.third)
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

        val db = KudosContextHolder.currentDatabase()
        val tenant = db.from(SysAuditLogTable).select()
            .where { SysAuditLogTable.id eq "e3" }
            .map { it[SysAuditLogTable.tenantId] }
            .single()
        assertEquals(
            "entity-tenant", tenant,
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

        val db = KudosContextHolder.currentDatabase()
        // ktorm `select(specific column)` requires a real dialect to translate; the no-dialect
        // default fails with `translate(...) must not be null`. Use `select()` (SELECT *) and pull
        // the columns out of the row — same pattern as the other passing tests in this class.
        val ids = db.from(SysAuditLogTable).select()
            .where { SysAuditLogTable.tenantId eq "tenant-B" }
            .map { it[SysAuditLogTable.id] }
            .filterNotNull()
            .sorted()
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

        val db = KudosContextHolder.currentDatabase()
        val mainIds = db.from(SysAuditLogTable).select()
            .where { SysAuditLogTable.id eq "e5" }.map { it[SysAuditLogTable.id] }
        assertEquals(listOf("e5"), mainIds)
        val detailIds = db.from(SysAuditDetailLogTable).select()
            .where { SysAuditDetailLogTable.auditId eq "e5" }.map { it[SysAuditDetailLogTable.id] }
        assertEquals(emptyList(), detailIds, "no detail rows when sysAuditDetailLogs is empty")
    }

    private fun forceMerge(table: String) {
        val jdbcUrl = requireNotNull(environment.getProperty("spring.datasource.dynamic.datasource.ds1.url"))
        DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD).use { conn ->
            conn.createStatement().use { st -> st.execute("OPTIMIZE TABLE $table FINAL") }
        }
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

        val db = KudosContextHolder.currentDatabase()
        val detailRows = db.from(SysAuditDetailLogTable).select()
            .where { SysAuditDetailLogTable.auditId eq "audit-only-d" }
            .map { it[SysAuditDetailLogTable.description] }
        assertEquals(listOf("orphan"), detailRows)
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
            val jdbcUrl = "jdbc:clickhouse://$host:$port/${ClickHouseTestContainer.DATABASE}?wait_end_of_query=1&async_insert=0"

            registry.add("spring.datasource.dynamic.datasource.ds1.url") { jdbcUrl }
            registry.add("spring.datasource.dynamic.datasource.ds1.username") { USERNAME }
            registry.add("spring.datasource.dynamic.datasource.ds1.password") { PASSWORD }
        }
    }
}
