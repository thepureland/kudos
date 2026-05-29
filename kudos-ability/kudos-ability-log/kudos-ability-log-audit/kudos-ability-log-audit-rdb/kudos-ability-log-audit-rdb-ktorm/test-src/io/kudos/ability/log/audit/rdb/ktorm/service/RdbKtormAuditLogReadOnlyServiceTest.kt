package io.kudos.ability.log.audit.rdb.ktorm.service

import io.kudos.ability.data.rdb.ktorm.datasource.currentDatabase
import io.kudos.ability.log.audit.common.api.IAuditLogReadOnlyService
import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.entity.AuditLogQuery
import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import io.kudos.ability.log.audit.rdb.ktorm.table.SysAuditDetailLogTable
import io.kudos.ability.log.audit.rdb.ktorm.table.SysAuditLogTable
import io.kudos.context.core.KudosContextHolder
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.ktorm.dsl.deleteAll
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.Date
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * End-to-end tests for [RdbKtormAuditLogReadOnlyService] — H2 in-memory + real Ktorm.
 *
 * Seeds rows via [IAuditService] so the schema, default values, and date coercion run through the
 * production write path. Each test calls [cleanTables] first so paging assertions don't fight stale
 * fixtures from neighboring tests.
 *
 * Covers:
 *  - findById hit / miss
 *  - findDetailById matched by auditId
 *  - pagingSearch with empty filter (matches everything) + ordering invariant (operate_time DESC)
 *  - filters: tenantId / operatorId / operateTypeId / time range / descriptionLike
 *  - page slicing + total invariants (total is the *unsliced* count)
 *  - empty filter on an empty table returns AuditLogPage.empty
 *  - pageNo / pageSize coercion (< 1 clamps to 1)
 */
@EnableKudosTest(properties = ["spring.flyway.enabled=false"])
internal open class RdbKtormAuditLogReadOnlyServiceTest {

    @Resource
    private lateinit var auditService: IAuditService

    @Resource(name = "rdbKtormAuditLogReadOnlyService")
    private lateinit var readOnlyService: IAuditLogReadOnlyService

    @Resource
    private lateinit var transactionManager: PlatformTransactionManager

    @BeforeTest
    fun cleanTables() {
        // Hikari is configured with `is-auto-commit: false`, so a bare deleteAll() outside a
        // transaction chain would silently never reach the wire — rows from this and sibling test
        // classes (RdbKtormAuditServiceTest) would accumulate and break pagingSearch total
        // assertions. Ktorm's connection-level useTransaction is also unavailable here because
        // Database.connectWithSpringSupport hands transaction control to Spring. Resolution: open
        // a programmatic Spring transaction via TransactionTemplate so the deletes commit cleanly.
        TransactionTemplate(transactionManager).execute {
            val db = KudosContextHolder.currentDatabase()
            db.deleteAll(SysAuditLogTable)
            db.deleteAll(SysAuditDetailLogTable)
        }
    }

    @Test
    fun findById_returnsVo_whenIdExists() {
        seed("ro-1", tenant = "t-1", operator = "u-1", desc = "create user", detailId = "rod-1")

        val found = readOnlyService.findById("ro-1")

        assertNotNull(found)
        assertEquals("ro-1", found.id)
        assertEquals("t-1", found.tenantId)
        assertEquals("u-1", found.operatorId)
        assertEquals("create user", found.description)
    }

    @Test
    fun findById_returnsNull_whenIdAbsent() {
        // Missing ids are a normal admin-side signal — audit retention may have purged the row;
        // throwing here would force every caller into a try/catch.
        assertNull(readOnlyService.findById("ro-does-not-exist"))
    }

    @Test
    fun findDetailById_matchesByAuditId() {
        seed("ro-2", tenant = "t-1", operator = "u-1", desc = "x", detailId = "rod-2", detailDesc = "PATCH /x")

        val detail = readOnlyService.findDetailById("ro-2")

        assertNotNull(detail)
        assertEquals("rod-2", detail.id)
        assertEquals("ro-2", detail.auditId)
        assertEquals("PATCH /x", detail.description)
    }

    @Test
    fun pagingSearch_emptyFilter_returnsEverythingOrderedByOperateTimeDesc() {
        // Seed three rows with increasing operate_time so DESC ordering is visually obvious in the result.
        val t0 = LocalDateTime.now().minusMinutes(30)
        seed("ro-3a", operator = "u-1", desc = "first",  operateTime = t0)
        seed("ro-3b", operator = "u-2", desc = "second", operateTime = t0.plusMinutes(10))
        seed("ro-3c", operator = "u-3", desc = "third",  operateTime = t0.plusMinutes(20))

        val page = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 1, pageSize = 10)

        assertEquals(3L, page.total)
        assertEquals(
            listOf("ro-3c", "ro-3b", "ro-3a"), page.items.map { it.id },
            "rows must come back operate_time DESC so admin 'recent first' view doesn't need a sort param",
        )
    }

    @Test
    fun pagingSearch_filterByTenantId() {
        seed("ro-4a", tenant = "t-A", operator = "u-1")
        seed("ro-4b", tenant = "t-B", operator = "u-2")

        val page = readOnlyService.pagingSearch(AuditLogQuery().apply { tenantId = "t-A" }, pageNo = 1, pageSize = 10)

        assertEquals(1L, page.total)
        assertEquals(listOf("ro-4a"), page.items.map { it.id })
    }

    @Test
    fun pagingSearch_filterByOperatorAndOperateTypeId_combinedWithAnd() {
        seed("ro-5a", operator = "u-1", operateTypeId = 2)
        seed("ro-5b", operator = "u-1", operateTypeId = 3) // wrong typeId
        seed("ro-5c", operator = "u-2", operateTypeId = 2) // wrong operator

        val query = AuditLogQuery().apply {
            operatorId = "u-1"
            operateTypeId = 2
        }
        val page = readOnlyService.pagingSearch(query, pageNo = 1, pageSize = 10)

        assertEquals(1L, page.total)
        assertEquals("ro-5a", page.items.single().id)
    }

    @Test
    fun pagingSearch_filterByOperateTimeRange_inclusiveLowerExclusiveUpper() {
        val base = LocalDateTime.of(2026, 5, 29, 12, 0, 0)
        seed("ro-6a", operator = "u-x", operateTime = base.minusMinutes(1)) // before window
        seed("ro-6b", operator = "u-x", operateTime = base)                 // at lower bound (inclusive → in)
        seed("ro-6c", operator = "u-x", operateTime = base.plusMinutes(5))  // inside
        seed("ro-6d", operator = "u-x", operateTime = base.plusMinutes(10)) // at upper bound (exclusive → out)

        val query = AuditLogQuery().apply {
            operateTimeFrom = base
            operateTimeTo = base.plusMinutes(10)
        }
        val page = readOnlyService.pagingSearch(query, pageNo = 1, pageSize = 10)

        // Expect: ro-6b (inclusive lower) + ro-6c. NOT ro-6a (before) and NOT ro-6d (at exclusive upper).
        assertEquals(2L, page.total)
        assertEquals(setOf("ro-6b", "ro-6c"), page.items.map { it.id }.toSet())
    }

    @Test
    fun pagingSearch_descriptionLike_fuzzyMatch() {
        seed("ro-7a", operator = "u-x", desc = "create user alice")
        seed("ro-7b", operator = "u-x", desc = "delete user bob")
        seed("ro-7c", operator = "u-x", desc = "reset password")

        val query = AuditLogQuery().apply { descriptionLike = "user" }
        val page = readOnlyService.pagingSearch(query, pageNo = 1, pageSize = 10)

        assertEquals(2L, page.total)
        assertEquals(setOf("ro-7a", "ro-7b"), page.items.map { it.id }.toSet())
    }

    @Test
    fun pagingSearch_emptyStringDescriptionLike_isTreatedAsNoFilter() {
        seed("ro-8a", operator = "u-x", desc = "anything")

        // An empty UI text input shouldn't accidentally generate `LIKE '%%'`. Contract: empty string == no filter.
        val page = readOnlyService.pagingSearch(AuditLogQuery().apply { descriptionLike = "" }, pageNo = 1, pageSize = 10)

        assertEquals(1L, page.total)
    }

    @Test
    fun pagingSearch_totalReflectsUnslicedMatch_butItemsRespectPageSize() {
        repeat(5) { seed("ro-9-$it", operator = "u-x", desc = "row-$it") }

        val page = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 1, pageSize = 2)

        // total is the full match count, even though the items slice carries only 2 rows.
        assertEquals(5L, page.total)
        assertEquals(2, page.items.size)
        assertEquals(1, page.pageNo)
        assertEquals(2, page.pageSize)
    }

    @Test
    fun pagingSearch_pageNoAndSizeBelowOneAreClampedToOne() {
        seed("ro-10", operator = "u-x", desc = "x")

        val page = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 0, pageSize = -3)

        assertEquals(1, page.pageNo, "pageNo < 1 must clamp to 1 — admin clients sometimes pass 0 by mistake")
        assertEquals(1, page.pageSize, "pageSize < 1 must clamp to 1 — caller would otherwise OOM rendering an empty slice loop")
        assertEquals(1L, page.total)
        assertEquals(1, page.items.size)
    }

    @Test
    fun pagingSearch_noMatch_returnsEmptyPage() {
        seed("ro-11", tenant = "t-A", operator = "u-1")

        val page = readOnlyService.pagingSearch(AuditLogQuery().apply { tenantId = "t-Z" }, pageNo = 1, pageSize = 10)

        assertTrue(page.items.isEmpty())
        assertEquals(0L, page.total)
        assertEquals(1, page.pageNo)
        assertEquals(10, page.pageSize)
    }

    @Test
    fun pagingSearch_operatorLike_fuzzyMatchesOperatorDisplayName() {
        seed("ro-12a", operator = "u-1", operatorName = "Alice Liu")
        seed("ro-12b", operator = "u-2", operatorName = "Bob Lin")
        seed("ro-12c", operator = "u-3", operatorName = "Charlie")

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { operatorLike = "Li" }, pageNo = 1, pageSize = 10,
        )

        // Matches both "Alice Liu" and "Bob Lin"; "Charlie" doesn't contain "Li" as substring.
        assertEquals(2L, page.total)
        assertEquals(setOf("ro-12a", "ro-12b"), page.items.map { it.id }.toSet())
    }

    @Test
    fun pagingSearch_moduleCodeLike_fuzzyMatchesPartialModulePath() {
        seed("ro-13a", operator = "u-x", moduleCode = "user.account")
        seed("ro-13b", operator = "u-x", moduleCode = "user.organization")
        seed("ro-13c", operator = "u-x", moduleCode = "sys.dict")

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { moduleCodeLike = "user" }, pageNo = 1, pageSize = 10,
        )

        assertEquals(2L, page.total)
        assertEquals(setOf("ro-13a", "ro-13b"), page.items.map { it.id }.toSet())
    }

    @Test
    fun pagingSearch_operateType_exactStringMatch() {
        seed("ro-14a", operator = "u-x", operateTypeText = "create")
        seed("ro-14b", operator = "u-x", operateTypeText = "delete")
        seed("ro-14c", operator = "u-x", operateTypeText = "createUser") // wouldn't match "create" exactly

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { operateType = "create" }, pageNo = 1, pageSize = 10,
        )

        assertEquals(1L, page.total)
        assertEquals("ro-14a", page.items.single().id)
    }

    // ----- Fixtures -----

    /**
     * Seeds a single audit-log main row (and optional detail). Sensible defaults so each test only
     * names the fields it actually cares about; named-arg call sites read like assertion specs.
     */
    private fun seed(
        id: String,
        tenant: String? = "t-default",
        operator: String? = "u-default",
        operatorName: String? = null,
        moduleCode: String? = "USER",
        operateTypeText: String? = null,
        operateTypeId: Int? = 2,
        desc: String? = "default",
        operateTime: LocalDateTime = LocalDateTime.now(),
        detailId: String? = null,
        detailDesc: String? = null,
    ) {
        val entity = SysAuditLogVo().apply {
            this.id = id
            this.entityId = "entity-$id"
            this.description = desc
            this.operateTypeId = operateTypeId
            this.operateType = operateTypeText ?: "type-$operateTypeId"
            this.moduleCode = moduleCode
            this.operator = operatorName
            this.operatorId = operator
            this.tenantId = tenant
            this.operateTime = Date.from(operateTime.atZone(java.time.ZoneId.systemDefault()).toInstant())
        }
        val detail: SysAuditDetailLogVo? = detailId?.let {
            SysAuditDetailLogVo().apply {
                this.id = it
                this.auditId = id
                this.description = detailDesc
                this.operateUrl = "/x"
            }
        }
        val model = SysAuditLogModel().apply {
            this.tenantId = tenant
            entities = mutableListOf(entity)
            sysAuditDetailLogs = detail?.let { mutableListOf<SysAuditDetailLogVo?>(it) } ?: mutableListOf()
        }
        auditService.submit(model)
    }
}
