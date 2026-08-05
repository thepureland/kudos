package io.kudos.ability.log.audit.rdb.clickhouse.service

import io.kudos.ability.log.audit.common.api.IAuditLogReadOnlyService
import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.entity.AuditLogQuery
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Integration tests for [RdbClickhouseAuditLogReadOnlyService] against a real ClickHouse 24.8.
 *
 * Reuses the existing test-container plumbing (same DDL + URL params as the write-side test).
 * Seeds rows via [auditService.submit] (proves the read-side actually round-trips the same
 * representation the write-side produces) and asserts on the read-side getters.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest(properties = ["spring.flyway.enabled=false"])
@EnabledIfDockerInstalled
internal open class RdbClickhouseAuditLogReadOnlyServiceTest {

    @Resource
    private lateinit var auditService: IAuditService

    @Resource
    private lateinit var readOnlyService: IAuditLogReadOnlyService

    @Resource
    private lateinit var environment: org.springframework.core.env.Environment

    @BeforeTest
    fun resetSchema() {
        val jdbcUrl = requireNotNull(environment.getProperty("spring.datasource.dynamic.datasource.ds1.url"))
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
    fun findById_returnsVoForExistingRow() {
        val model = newModel(tenantId = "T-find").apply {
            entities = mutableListOf(newEntity("rid-1", entityId = "u-100", desc = "create"))
        }
        auditService.submit(model)
        forceMerge("sys_audit_log")

        val vo: SysAuditLogVo = assertNotNull(readOnlyService.findById("rid-1"))
        assertEquals("rid-1", vo.id)
        assertEquals("u-100", vo.entityId)
        assertEquals("create", vo.description)
        assertEquals("T-find", vo.tenantId)
    }

    @Test
    fun findById_returnsNullForMissing() {
        assertNull(readOnlyService.findById("does-not-exist"))
    }

    @Test
    fun findDetailById_returnsDetailVoForExistingAudit() {
        val model = newModel(tenantId = "T").apply {
            entities = mutableListOf(newEntity("aid-1"))
            sysAuditDetailLogs = mutableListOf(newDetail("did-1", auditId = "aid-1", url = "/api/x", desc = "DETAIL"))
        }
        auditService.submit(model)
        forceMerge("sys_audit_detail_log")

        val detail: SysAuditDetailLogVo = assertNotNull(readOnlyService.findDetailById("aid-1"))
        assertEquals("did-1", detail.id)
        assertEquals("aid-1", detail.auditId)
        assertEquals("/api/x", detail.operateUrl)
        assertEquals("DETAIL", detail.description)
    }

    @Test
    fun findDetailById_returnsNullWhenAuditHasNoDetail() {
        // Audit row with no matching detail (e.g. backfill or aspect that didn't supply one).
        val model = newModel(tenantId = "T").apply {
            entities = mutableListOf(newEntity("no-detail-audit"))
            sysAuditDetailLogs = mutableListOf() // none
        }
        auditService.submit(model)
        forceMerge("sys_audit_log")

        assertNull(readOnlyService.findDetailById("no-detail-audit"))
    }

    @Test
    fun findDetailById_returnsNullForMissingAuditId() {
        assertNull(readOnlyService.findDetailById("does-not-exist"))
    }

    @Test
    fun pagingSearch_emptyTable_returnsEmptyPage() {
        val page = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 1, pageSize = 10)
        assertEquals(0L, page.total)
        assertEquals(emptyList(), page.items)
        assertEquals(1, page.pageNo)
        assertEquals(10, page.pageSize)
    }

    @Test
    fun pagingSearch_noFilters_returnsRowsByOperateTimeDesc() {
        val base = LocalDateTime.of(2026, 1, 1, 10, 0)
        seedDirectly(
            Triple("earliest", "T", base),
            Triple("middle", "T", base.plusHours(1)),
            Triple("latest", "T", base.plusHours(2)),
        )

        val page = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 1, pageSize = 10)
        assertEquals(3L, page.total)
        assertEquals(listOf("latest", "middle", "earliest"), page.items.map { it.id })
    }

    @Test
    fun pagingSearch_tenantId_filtersExact() {
        seedDirectly(
            Triple("t-a-1", "tenant-A", LocalDateTime.now()),
            Triple("t-a-2", "tenant-A", LocalDateTime.now()),
            Triple("t-b-1", "tenant-B", LocalDateTime.now()),
        )

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { tenantId = "tenant-A" }, pageNo = 1, pageSize = 10
        )
        assertEquals(2L, page.total)
        assertEquals(setOf("t-a-1", "t-a-2"), page.items.map { it.id }.toSet())
    }

    @Test
    fun pagingSearch_descriptionLike_isSubstringMatch() {
        seedDirectlyWithDesc(
            Triple("hit", "T", "update order fast"),
            Triple("partial", "T", "fast"),
            Triple("miss", "T", "slow"),
        )

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { descriptionLike = "fast" }, pageNo = 1, pageSize = 10
        )
        assertEquals(2L, page.total)
        assertEquals(setOf("hit", "partial"), page.items.map { it.id }.toSet())
    }

    @Test
    fun pagingSearch_descriptionLikeEmptyString_isTreatedAsNoFilter() {
        seedDirectly(
            Triple("any-1", "T", LocalDateTime.now()),
            Triple("any-2", "T", LocalDateTime.now()),
        )
        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { descriptionLike = "" }, pageNo = 1, pageSize = 10
        )
        assertEquals(2L, page.total, "empty descriptionLike must skip the filter, returning all rows")
    }

    @Test
    fun pagingSearch_operateTimeWindow_inclusiveLowerExclusiveUpper() {
        val t9 = LocalDateTime.of(2026, 1, 1, 9, 0)
        val t10 = LocalDateTime.of(2026, 1, 1, 10, 0)
        val t11 = LocalDateTime.of(2026, 1, 1, 11, 0)
        seedDirectly(
            Triple("at-9", "T", t9),
            Triple("at-10", "T", t10),
            Triple("at-11", "T", t11),
        )

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply {
                operateTimeFrom = t10
                operateTimeTo = t11
            },
            pageNo = 1, pageSize = 10,
        )
        assertEquals(1L, page.total)
        assertEquals("at-10", page.items.single().id)
    }

    @Test
    fun pagingSearch_pageNumberAndSize_slicesCorrectly() {
        val base = LocalDateTime.of(2026, 1, 1, 10, 0)
        val triples = (0..6).map { i -> Triple("r-$i", "T", base.plusMinutes(i.toLong())) }
        seedDirectly(*triples.toTypedArray())

        val page1 = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 1, pageSize = 3)
        val page2 = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 2, pageSize = 3)
        val page3 = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 3, pageSize = 3)

        assertEquals(7L, page1.total)
        assertEquals(listOf("r-6", "r-5", "r-4"), page1.items.map { it.id })
        assertEquals(listOf("r-3", "r-2", "r-1"), page2.items.map { it.id })
        assertEquals(listOf("r-0"), page3.items.map { it.id })
    }

    @Test
    fun pagingSearch_pageNoOrSizeBelowOne_clampToOne() {
        seedDirectly(Triple("only-one", "T", LocalDateTime.now()))
        val page = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 0, pageSize = -5)
        assertEquals(1, page.pageNo)
        assertEquals(1, page.pageSize)
        assertEquals(1L, page.total)
        assertEquals("only-one", page.items.single().id)
    }

    @Test
    fun pagingSearch_combinedFilters_areAnded() {
        // tenantId=T AND operatorId=alice AND descriptionLike "fast"
        seedDirectlyDetailed(listOf(
            SeedRow(id = "match", tenant = "T", operatorId = "alice", desc = "do something fast"),
            SeedRow(id = "wrong-tenant", tenant = "X", operatorId = "alice", desc = "fast"),
            SeedRow(id = "wrong-op", tenant = "T", operatorId = "bob", desc = "fast"),
            SeedRow(id = "wrong-desc", tenant = "T", operatorId = "alice", desc = "slow"),
        ))

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply {
                tenantId = "T"
                operatorId = "alice"
                descriptionLike = "fast"
            },
            pageNo = 1, pageSize = 10,
        )
        assertEquals(1L, page.total)
        assertEquals("match", page.items.single().id)
    }

    @Test
    fun findById_mapsEveryColumn_fullyPopulatedRow() {
        seedFullRow(id = "full-1")

        val vo = assertNotNull(readOnlyService.findById("full-1"))
        assertEquals("full-1", vo.id)
        assertEquals("ent-42", vo.entityId)
        assertEquals(7, vo.operateTypeId)
        assertEquals("update", vo.operateType)
        assertEquals(3, vo.moduleId)
        assertEquals("用户管理", vo.moduleName)
        assertEquals("USER", vo.moduleCode)
        assertEquals("full row 描述", vo.description)
        assertEquals("alice O'Neil", vo.operator)
        assertEquals("op-1", vo.operatorId)
        assertEquals("ADMIN", vo.operatorUserType)
        assertEquals("T-full", vo.tenantId)
        assertEquals("T-source", vo.sourceTenantId)
        assertEquals("sys-x", vo.subSysCode)
        assertNotNull(vo.operateTime, "operate_time must round-trip to a non-null Date")
        assertEquals(3232235777L, vo.operateIp, "operate_ip is a BIGINT (IPv4 as Int64)")
        assertEquals("ipv4", vo.operateIpDictCode)
        assertEquals("Windows 11", vo.clientOs)
        assertEquals("Chrome", vo.clientBrowser)
        assertEquals("POST", vo.requestType)
    }

    @Test
    fun findById_minimalRow_nullableColumnsComeBackNull() {
        // Row carrying only the NOT NULL columns: every Nullable(...) column must surface as
        // Kotlin null — in particular the Int/Long ones, where ResultSet.getInt/getLong would
        // otherwise lie with 0 (getNullableInt / getNullableLong wasNull guard).
        seedDirectly(Triple("min-1", "T-min", LocalDateTime.of(2026, 1, 1, 12, 0)))

        val vo = assertNotNull(readOnlyService.findById("min-1"))
        assertNull(vo.entityId)
        assertNull(vo.operateTypeId, "NULL Int32 must map to null, not 0")
        assertNull(vo.moduleId)
        assertNull(vo.operateIp, "NULL Int64 must map to null, not 0L")
        assertNull(vo.description)
        assertNull(vo.operator)
        assertNull(vo.operatorId)
        assertNull(vo.operatorUserType)
        assertNull(vo.sourceTenantId)
        assertNull(vo.subSysCode)
        assertNull(vo.operateIpDictCode)
        assertNull(vo.clientOs)
        assertNull(vo.clientBrowser)
        assertNull(vo.requestType)
        assertEquals("T-min", vo.tenantId)
    }

    @Test
    fun findDetailById_mapsEveryColumn_fullyPopulatedRow() {
        val jdbcUrl = requireNotNull(environment.getProperty("spring.datasource.dynamic.datasource.ds1.url"))
        DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD).use { conn ->
            conn.createStatement().use { st ->
                st.execute(
                    "INSERT INTO sys_audit_detail_log " +
                        "(id, audit_id, operate_url, string_params, object_params, request_referer, request_form_data, description) " +
                        "VALUES ('fd-1', 'fa-1', '/api/users/42', 'p1=v1&p2=v2', '{\"k\":\"v\"}', " +
                        "'https://admin.example.com/users', 'name=张三&age=30', 'detail 描述')",
                )
            }
        }
        forceMerge("sys_audit_detail_log")

        val detail = assertNotNull(readOnlyService.findDetailById("fa-1"))
        assertEquals("fd-1", detail.id)
        assertEquals("fa-1", detail.auditId)
        assertEquals("/api/users/42", detail.operateUrl)
        assertEquals("p1=v1&p2=v2", detail.stringParams)
        assertEquals("{\"k\":\"v\"}", detail.objectParams)
        assertEquals("https://admin.example.com/users", detail.requestReferer)
        assertEquals("name=张三&age=30", detail.requestFormData)
        assertEquals("detail 描述", detail.description)
    }

    @Test
    fun pagingSearch_remainingExactAndLikeFilters_eachNarrowsTheResult() {
        // Covers the buildWhereClause branches the other tests don't touch: sourceTenantId,
        // subSysCode, moduleCode (exact), operateTypeId, operatorUserType, operatorLike,
        // moduleCodeLike, operateType (string), entityId. One fully matching row + one decoy
        // per dimension proves both the SQL fragment and its binder line up positionally.
        seedFullRow(id = "all-match")
        seedFullRow(id = "off-source", sourceTenantId = "OTHER")
        seedFullRow(id = "off-subsys", subSysCode = "sys-other")
        seedFullRow(id = "off-module", moduleCode = "ORDER")
        seedFullRow(id = "off-type-id", operateTypeId = 99)
        seedFullRow(id = "off-user-type", operatorUserType = "GUEST")
        seedFullRow(id = "off-operator", operator = "bob")
        seedFullRow(id = "off-op-type", operateType = "delete")
        seedFullRow(id = "off-entity", entityId = "ent-other")

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply {
                sourceTenantId = "T-source"
                subSysCode = "sys-x"
                moduleCode = "USER"
                operateTypeId = 7
                operatorUserType = "ADMIN"
                operatorLike = "lice O'Nei" // substring of "alice O'Neil"; quote also proves binding escapes
                moduleCodeLike = "SE"       // substring of "USER"
                operateType = "update"
                entityId = "ent-42"
            },
            pageNo = 1, pageSize = 10,
        )
        assertEquals(1L, page.total)
        assertEquals("all-match", page.items.single().id)
    }

    @Test
    fun pagingSearch_emptyLikeStrings_skipTheirFilters() {
        // operatorLike / moduleCodeLike / operateType use takeIf { isNotEmpty() } — empty input
        // (cleared UI field) must behave like null, not LIKE '%%' / = ''.
        seedDirectly(
            Triple("skip-1", "T", LocalDateTime.of(2026, 2, 1, 8, 0)),
            Triple("skip-2", "T", LocalDateTime.of(2026, 2, 1, 9, 0)),
        )
        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply {
                operatorLike = ""
                moduleCodeLike = ""
                operateType = ""
            },
            pageNo = 1, pageSize = 10,
        )
        assertEquals(2L, page.total, "empty like/operateType strings must skip their filters")
    }

    /** Insert one fully populated main-table row via JDBC, with per-column overrides for decoys. */
    private fun seedFullRow(
        id: String,
        tenant: String = "T-full",
        sourceTenantId: String = "T-source",
        subSysCode: String = "sys-x",
        moduleCode: String = "USER",
        operateTypeId: Int = 7,
        operatorUserType: String = "ADMIN",
        operator: String = "alice O'Neil",
        operateType: String = "update",
        entityId: String = "ent-42",
    ) {
        val jdbcUrl = requireNotNull(environment.getProperty("spring.datasource.dynamic.datasource.ds1.url"))
        DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD).use { conn ->
            conn.prepareStatement(
                "INSERT INTO sys_audit_log (id, entity_id, operator_id, operator, operate_time, operate_type_id, " +
                    "operate_type, module_name, module_code, module_id, description, request_type, client_os, " +
                    "client_browser, operator_user_type, operate_ip, operate_ip_dict_code, tenant_id, " +
                    "source_tenant_id, sub_sys_code) " +
                    "VALUES (?, ?, 'op-1', ?, '2026-03-01 10:30:00', ?, ?, '用户管理', ?, 3, 'full row 描述', " +
                    "'POST', 'Windows 11', 'Chrome', ?, 3232235777, 'ipv4', ?, ?, ?)",
            ).use { ps ->
                ps.setString(1, id)
                ps.setString(2, entityId)
                ps.setString(3, operator)
                ps.setInt(4, operateTypeId)
                ps.setString(5, operateType)
                ps.setString(6, moduleCode)
                ps.setString(7, operatorUserType)
                ps.setString(8, tenant)
                ps.setString(9, sourceTenantId)
                ps.setString(10, subSysCode)
                ps.execute()
            }
        }
        forceMerge("sys_audit_log")
    }

    private fun newModel(tenantId: String? = null, subSysCode: String? = null): SysAuditLogModel =
        SysAuditLogModel().apply {
            this.tenantId = tenantId
            this.subSysCode = subSysCode
        }

    private fun newEntity(id: String, entityId: String? = null, desc: String? = null): SysAuditLogVo =
        SysAuditLogVo().apply {
            this.id = id
            this.entityId = entityId
            this.description = desc
            this.operateTypeId = 2
            this.operateType = "create"
            this.moduleCode = "USER"
            this.operateTime = Date()
        }

    private fun newDetail(id: String, auditId: String, url: String, desc: String): SysAuditDetailLogVo =
        SysAuditDetailLogVo().apply {
            this.id = id
            this.auditId = auditId
            this.operateUrl = url
            this.description = desc
        }

    /**
     * Insert rows directly via JDBC instead of through the write service. The write path uses
     * `Date()` for operate_time, which collapses all rows to the same instant — useless for
     * "order by operate_time desc" assertions. Direct INSERT lets us pin per-row timestamps.
     */
    private fun seedDirectly(vararg rows: Triple<String, String, LocalDateTime>) {
        val jdbcUrl = requireNotNull(environment.getProperty("spring.datasource.dynamic.datasource.ds1.url"))
        DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD).use { conn ->
            conn.createStatement().use { st ->
                rows.forEach { (id, tenant, time) ->
                    val tsLiteral = "'${time.format(CLICKHOUSE_TS_FORMAT)}'"
                    st.execute(
                        "INSERT INTO sys_audit_log (id, tenant_id, operate_time, operate_type, module_code) " +
                            "VALUES ('$id', '$tenant', $tsLiteral, 'create', 'USER')",
                    )
                }
            }
        }
        forceMerge("sys_audit_log")
    }

    private fun seedDirectlyWithDesc(vararg rows: Triple<String, String, String>) {
        val jdbcUrl = requireNotNull(environment.getProperty("spring.datasource.dynamic.datasource.ds1.url"))
        val now = LocalDateTime.now().format(CLICKHOUSE_TS_FORMAT)
        DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD).use { conn ->
            conn.createStatement().use { st ->
                rows.forEach { (id, tenant, desc) ->
                    val descEscaped = desc.replace("'", "''")
                    st.execute(
                        "INSERT INTO sys_audit_log (id, tenant_id, operate_time, description) " +
                            "VALUES ('$id', '$tenant', '$now', '$descEscaped')",
                    )
                }
            }
        }
        forceMerge("sys_audit_log")
    }

    private fun seedDirectlyDetailed(rows: List<SeedRow>) {
        val jdbcUrl = requireNotNull(environment.getProperty("spring.datasource.dynamic.datasource.ds1.url"))
        val now = LocalDateTime.now().format(CLICKHOUSE_TS_FORMAT)
        DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD).use { conn ->
            conn.createStatement().use { st ->
                rows.forEach { row ->
                    val descEscaped = row.desc.replace("'", "''")
                    st.execute(
                        "INSERT INTO sys_audit_log (id, tenant_id, operator_id, description, operate_time) " +
                            "VALUES ('${row.id}', '${row.tenant}', '${row.operatorId}', '$descEscaped', '$now')",
                    )
                }
            }
        }
        forceMerge("sys_audit_log")
    }

    private fun forceMerge(table: String) {
        val jdbcUrl = requireNotNull(environment.getProperty("spring.datasource.dynamic.datasource.ds1.url"))
        DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD).use { conn ->
            conn.createStatement().use { st -> st.execute("OPTIMIZE TABLE $table FINAL") }
        }
    }

    private data class SeedRow(val id: String, val tenant: String, val operatorId: String, val desc: String)

    private fun LocalDateTime.toDate(): Date = Date.from(this.atZone(ZoneId.systemDefault()).toInstant())

    companion object {
        private const val USERNAME = "default"
        private const val PASSWORD = ""

        // ClickHouse DateTime literal format — `yyyy-MM-dd HH:mm:ss`. LocalDateTime.toString omits
        // the seconds when they're zero (e.g. "2026-01-01T10:00") which ClickHouse can't parse.
        private val CLICKHOUSE_TS_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

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
            val running = ClickHouseTestContainer.getRunningContainer()
                ?: error("ClickHouse container failed to start")
            val httpPort = running.ports.first { it.privatePort == 8123 }
            val host = requireNotNull(httpPort.ip)
            val port = requireNotNull(httpPort.publicPort)
            // clickhouse-jdbc 0.9.x (client-v2) rejects unknown raw URL properties; server
            // settings need the `clickhouse_setting_` prefix to be forwarded.
            val jdbcUrl = "jdbc:clickhouse://$host:$port/${ClickHouseTestContainer.DATABASE}" +
                "?clickhouse_setting_wait_end_of_query=1&clickhouse_setting_async_insert=0"
            registry.add("spring.datasource.dynamic.datasource.ds1.url") { jdbcUrl }
            registry.add("spring.datasource.dynamic.datasource.ds1.username") { USERNAME }
            registry.add("spring.datasource.dynamic.datasource.ds1.password") { PASSWORD }
        }
    }
}
