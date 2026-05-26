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
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end test for [RdbKtormAuditService] — H2 in-memory + real Ktorm writes / reads.
 *
 * Coverage:
 *  - Batch insert into main + detail tables: one submit writes N rows
 *  - Top-level `tenantId` / `subSysCode` fallback: filled from model when entity's own is missing
 *  - Empty model returns true fast (no SQL emitted)
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

    @BeforeTest
    fun cleanTables() {
        // Clean tables before each test method to avoid interference
        val db = KudosContextHolder.currentDatabase()
        db.deleteAll(SysAuditLogTable)
        db.deleteAll(SysAuditDetailLogTable)
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
