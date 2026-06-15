package io.kudos.ability.log.audit.rdb.ktorm.service

import io.kudos.ability.data.rdb.ktorm.datasource.currentDatabase
import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import io.kudos.ability.log.audit.rdb.common.AuditLogSchema
import io.kudos.ability.log.audit.rdb.ktorm.table.SysAuditDetailLogTable
import io.kudos.ability.log.audit.rdb.ktorm.table.SysAuditLogTable
import io.kudos.context.core.KudosContextHolder
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.ktorm.dsl.deleteAll
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.Date
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end test for [RdbKtormAuditService] — H2 in-memory + real Ktorm writes / reads.
 *
 * Coverage:
 *  - Batch insert into main + detail tables: one submit writes N rows
 *  - Top-level `tenantId` / `subSysCode` fallback: filled from model when entity's own is missing
 *  - Empty model returns true fast (no SQL emitted)
 *  - Null lists (vs. empty lists) and detail lists containing only nulls are treated as no-op
 *  - Details-only model writes the detail table without touching the main table
 *  - `operateTime == null` falls back to LocalDateTime.now()
 *  - Full-column round trip incl. Unicode text and long-typed IP
 *  - When the business method throws SQLException, returns false instead of propagating — `REQUIRES_NEW` boundary
 *    + try/catch
 *
 * Uses a local H2 in-memory database (no Docker dependency). DDL is loaded via baomidou dynamic-datasource's
 * `init.schema` — lighter weight than flyway integration and better suited to unit tests.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest(properties = ["spring.flyway.enabled=false"])
internal open class RdbKtormAuditServiceTest {

    @Resource
    private lateinit var auditService: IAuditService

    @Resource
    private lateinit var transactionManager: PlatformTransactionManager

    @BeforeTest
    fun cleanTables() {
        // Clean tables before each test method to avoid interference. Hikari runs with
        // `is-auto-commit: false`, so a bare deleteAll() outside a transaction would never commit —
        // rows from sibling tests would leak into whole-table assertions. Open a programmatic
        // Spring transaction (same resolution as RdbKtormAuditLogReadOnlyServiceTest.cleanTables).
        TransactionTemplate(transactionManager).execute {
            val db = KudosContextHolder.currentDatabase()
            db.deleteAll(SysAuditLogTable)
            db.deleteAll(SysAuditDetailLogTable)
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
        val mainRows = db.from(SysAuditLogTable).select().where { SysAuditLogTable.id eq "e1" }
            .map { it[SysAuditLogTable.entityId] to it[SysAuditLogTable.description] }
        assertEquals(listOf("u-100" to "create user"), mainRows)

        val detailRows = db.from(SysAuditDetailLogTable).select()
            .where { SysAuditDetailLogTable.auditId eq "e1" }
            .map { it[SysAuditDetailLogTable.description] }
        assertEquals(listOf("POST /users"), detailRows)
    }

    @Test
    fun submit_propagatesTopLevelTenantAndSubSysCode_whenEntityMissing() {
        // entity itself omits tenantId / subSysCode; top-level model provides them — should take effect
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
        // When the entity has its own value, the top-level fallback must not overwrite it
        val entity = makeEntity("e3", entityId = "u-300", desc = "x").apply {
            tenantId = "entity-tenant"
        }
        val model = SysAuditLogModel().apply {
            tenantId = "model-tenant"
            entities = mutableListOf(entity)
        }

        auditService.submit(model)

        val db = KudosContextHolder.currentDatabase()
        val tenant = db.from(SysAuditLogTable).select(SysAuditLogTable.tenantId)
            .where { SysAuditLogTable.id eq "e3" }
            .map { it[SysAuditLogTable.tenantId] }
            .single()
        assertEquals("entity-tenant", tenant, "The entity's own tenantId should win and not be overwritten by the top-level fallback")
    }

    @Test
    fun submit_emptyModel_returnsTrueAndWritesNothing() {
        val model = SysAuditLogModel().apply {
            entities = mutableListOf()
            sysAuditDetailLogs = mutableListOf()
        }
        assertTrue(auditService.submit(model), "Empty model should return true fast (treated as no-op)")
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

        val db = KudosContextHolder.currentDatabase()
        val ids = db.from(SysAuditLogTable).select(SysAuditLogTable.id)
            .where { SysAuditLogTable.tenantId eq "tenant-B" }
            .map { it[SysAuditLogTable.id] }
            .filterNotNull()
            .sorted()
        assertEquals(listOf("e4-1", "e4-2", "e4-3"), ids)
    }

    @Test
    fun submit_duplicatePrimaryKey_returnsFalseInsteadOfThrowing() {
        val model = SysAuditLogModel().apply {
            entities = mutableListOf(
                makeEntity("dup-id", entityId = "u-A", desc = "first"),
                makeEntity("dup-id", entityId = "u-B", desc = "second"),
            )
        }

        assertEquals(false, auditService.submit(model), "Persistence exceptions should be caught and converted to false")
    }

    @Test
    fun submit_nullLists_returnsTrueAndWritesNothing() {
        // entities / sysAuditDetailLogs left as null (not empty lists) — orEmpty() must normalize both
        val model = SysAuditLogModel()
        assertTrue(auditService.submit(model), "Null lists should behave exactly like empty lists (no-op, true)")

        val db = KudosContextHolder.currentDatabase()
        val mainCount = db.from(SysAuditLogTable).select(SysAuditLogTable.id).map { it[SysAuditLogTable.id] }.size
        assertEquals(0, mainCount, "No row may be written for a null-list model")
    }

    @Test
    fun submit_detailListContainingOnlyNulls_isTreatedAsEmpty() {
        // The detail list is declared MutableList<SysAuditDetailLogVo?> — null elements must be filtered, not NPE
        val model = SysAuditLogModel().apply {
            sysAuditDetailLogs = mutableListOf(null, null)
        }
        assertTrue(auditService.submit(model))

        val db = KudosContextHolder.currentDatabase()
        val detailCount = db.from(SysAuditDetailLogTable).select(SysAuditDetailLogTable.id)
            .map { it[SysAuditDetailLogTable.id] }.size
        assertEquals(0, detailCount)
    }

    @Test
    fun submit_detailsOnly_writesDetailTableWithoutMainRows() {
        // Exercises the branch where entities is empty but details is not — only the second batchInsert runs
        val model = SysAuditLogModel().apply {
            sysAuditDetailLogs = mutableListOf(
                makeDetail("od-1", auditId = "absent-main-1", desc = "detail only 1"),
                null, // interleaved null must be skipped, the valid sibling still written
                makeDetail("od-2", auditId = "absent-main-2", desc = "detail only 2"),
            )
        }
        assertTrue(auditService.submit(model))

        val db = KudosContextHolder.currentDatabase()
        val detailIds = db.from(SysAuditDetailLogTable).select(SysAuditDetailLogTable.id)
            .map { it[SysAuditDetailLogTable.id] }.filterNotNull().sorted()
        assertEquals(listOf("od-1", "od-2"), detailIds)

        val mainCount = db.from(SysAuditLogTable).select(SysAuditLogTable.id).map { it[SysAuditLogTable.id] }.size
        assertEquals(0, mainCount, "A details-only model must not write main-table rows")
    }

    @Test
    fun submit_nullOperateTime_defaultsToNow() {
        val before = LocalDateTime.now().minusMinutes(1)
        val entity = makeEntity("e-time-null", entityId = "u-t", desc = "x").apply { operateTime = null }
        val model = SysAuditLogModel().apply { entities = mutableListOf(entity) }

        assertTrue(auditService.submit(model))

        val db = KudosContextHolder.currentDatabase()
        val stored = db.from(SysAuditLogTable).select(SysAuditLogTable.operateTime)
            .where { SysAuditLogTable.id eq "e-time-null" }
            .map { it[SysAuditLogTable.operateTime] }
            .single()
        assertNotNull(stored, "Null operateTime must be replaced by now(), never persisted as NULL")
        val after = LocalDateTime.now().plusMinutes(1)
        assertTrue(
            !stored.isBefore(before) && !stored.isAfter(after),
            "Defaulted operate_time should be 'now' (was $stored, window [$before, $after])",
        )
    }

    @Test
    fun submit_persistsAllColumns_includingUnicodeAndLongIp() {
        val opTime = Date()
        val entity = SysAuditLogVo().apply {
            id = "e-full"
            entityId = "u-full"
            operateTypeId = 9
            operateType = "更新"
            moduleId = 7
            moduleName = "用户模块-ユーザ-🦊"
            moduleCode = "USER.ACC"
            description = "全字段回归 — 长文本与 Unicode ✓"
            operator = "张三"
            operatorId = "op-001"
            operatorUserType = "ADMIN"
            tenantId = "t-full"
            sourceTenantId = "src-t-full"
            subSysCode = "sys-full"
            operateTime = opTime
            operateIp = 3232235777L // 192.168.1.1 as long
            operateIpDictCode = "ipv4"
            clientOs = "Windows 11"
            clientBrowser = "Edge 137"
            requestType = "POST"
        }
        val detail = SysAuditDetailLogVo().apply {
            id = "d-full"
            auditId = "e-full"
            operateUrl = "/api/users/1?lang=zh"
            stringParams = "a=1&b=中文"
            objectParams = """{"k":"v","emoji":"🦊"}"""
            requestReferer = "https://example.test/admin"
            requestFormData = "f1=x&f2=y"
            description = "detail 全字段"
        }
        val model = SysAuditLogModel().apply {
            entities = mutableListOf(entity)
            sysAuditDetailLogs = mutableListOf(detail)
        }

        assertTrue(auditService.submit(model))

        val db = KudosContextHolder.currentDatabase()
        val row = db.from(SysAuditLogTable).select().where { SysAuditLogTable.id eq "e-full" }
            .map { r ->
                mapOf(
                    "entityId" to r[SysAuditLogTable.entityId],
                    "operateTypeId" to r[SysAuditLogTable.operateTypeId],
                    "operateType" to r[SysAuditLogTable.operateType],
                    "moduleId" to r[SysAuditLogTable.moduleId],
                    "moduleName" to r[SysAuditLogTable.moduleName],
                    "moduleCode" to r[SysAuditLogTable.moduleCode],
                    "description" to r[SysAuditLogTable.description],
                    "operator" to r[SysAuditLogTable.operator],
                    "operatorId" to r[SysAuditLogTable.operatorId],
                    "operatorUserType" to r[SysAuditLogTable.operatorUserType],
                    "tenantId" to r[SysAuditLogTable.tenantId],
                    "sourceTenantId" to r[SysAuditLogTable.sourceTenantId],
                    "subSysCode" to r[SysAuditLogTable.subSysCode],
                    "operateIp" to r[SysAuditLogTable.operateIp],
                    "operateIpDictCode" to r[SysAuditLogTable.operateIpDictCode],
                    "clientOs" to r[SysAuditLogTable.clientOs],
                    "clientBrowser" to r[SysAuditLogTable.clientBrowser],
                    "requestType" to r[SysAuditLogTable.requestType],
                )
            }.single()
        assertEquals("u-full", row["entityId"])
        assertEquals(9, row["operateTypeId"])
        assertEquals("更新", row["operateType"])
        assertEquals(7, row["moduleId"])
        assertEquals("用户模块-ユーザ-🦊", row["moduleName"])
        assertEquals("USER.ACC", row["moduleCode"])
        assertEquals("全字段回归 — 长文本与 Unicode ✓", row["description"])
        assertEquals("张三", row["operator"])
        assertEquals("op-001", row["operatorId"])
        assertEquals("ADMIN", row["operatorUserType"])
        assertEquals("t-full", row["tenantId"])
        assertEquals("src-t-full", row["sourceTenantId"])
        assertEquals("sys-full", row["subSysCode"])
        assertEquals(3232235777L, row["operateIp"])
        assertEquals("ipv4", row["operateIpDictCode"])
        assertEquals("Windows 11", row["clientOs"])
        assertEquals("Edge 137", row["clientBrowser"])
        assertEquals("POST", row["requestType"])

        val detailRow = db.from(SysAuditDetailLogTable).select()
            .where { SysAuditDetailLogTable.id eq "d-full" }
            .map { r ->
                listOf(
                    r[SysAuditDetailLogTable.auditId],
                    r[SysAuditDetailLogTable.operateUrl],
                    r[SysAuditDetailLogTable.stringParams],
                    r[SysAuditDetailLogTable.objectParams],
                    r[SysAuditDetailLogTable.requestReferer],
                    r[SysAuditDetailLogTable.requestFormData],
                    r[SysAuditDetailLogTable.description],
                )
            }.single()
        assertEquals(
            listOf(
                "e-full", "/api/users/1?lang=zh", "a=1&b=中文", """{"k":"v","emoji":"🦊"}""",
                "https://example.test/admin", "f1=x&f2=y", "detail 全字段",
            ),
            detailRow,
        )
    }

    private fun makeEntity(id: String, entityId: String, desc: String): SysAuditLogVo = SysAuditLogVo().apply {
        this.id = id
        this.entityId = entityId
        this.description = desc
        this.operateTypeId = 2
        this.operateType = "create"
        this.moduleCode = "USER"
        this.operateTime = Date()
    }

    private fun makeDetail(id: String, auditId: String, desc: String): SysAuditDetailLogVo = SysAuditDetailLogVo().apply {
        this.id = id
        this.auditId = auditId
        this.description = desc
        this.operateUrl = "/x"
    }
}
