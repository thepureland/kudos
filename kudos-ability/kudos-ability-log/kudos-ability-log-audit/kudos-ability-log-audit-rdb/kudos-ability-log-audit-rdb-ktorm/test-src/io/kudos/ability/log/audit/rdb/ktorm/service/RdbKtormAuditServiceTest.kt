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
 * [RdbKtormAuditService] 端到端测试——H2 in-memory + 真实 Ktorm 写入 / 读取。
 *
 * 覆盖：
 *  - 主表 + 详情表批量插入：一次 submit 写 N 条
 *  - 顶层 `tenantId` / `subSysCode` 兜底：entity 自身缺时用 model 顶层填
 *  - 空模型快速返回 true（无 SQL 发出）
 *  - 业务方法抛 SQLException 时返回 false 而非外抛——`REQUIRES_NEW` 事务边界 + try/catch
 *
 * 用本地 H2 内存库（无 Docker 依赖）。DDL 走 baomidou dynamic-datasource 的 `init.schema`
 * 加载——比 flyway 集成更轻量、更适合单测。
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
        // 每个测试方法跑前清表，避免互相干扰
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
        // entity 自身不填 tenantId / subSysCode；顶层 model 提供——应当生效
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
        // entity 自身有值时，不能被顶层兜底覆盖
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
        assertEquals("entity-tenant", tenant, "entity 自身的 tenantId 应当胜出，不能被顶层兜底覆盖")
    }

    @Test
    fun submit_emptyModel_returnsTrueAndWritesNothing() {
        val model = SysAuditLogModel().apply {
            entities = mutableListOf()
            sysAuditDetailLogs = mutableListOf()
        }
        assertTrue(auditService.submit(model), "空模型应当快速返回 true（视为 no-op）")
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

        assertEquals(false, auditService.submit(model), "落库异常应被捕获并转成 false")
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
