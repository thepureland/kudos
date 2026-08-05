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
import org.ktorm.dsl.insert
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
 *  - findDetailById miss returns null
 *  - full column mapping of both row mappers (main vo + detail vo), incl. Unicode and long IP
 *  - remaining predicate branches: sourceTenantId / subSysCode / moduleCode exact / operatorUserType /
 *    entityId / operateTimeFrom-only / operateTimeTo-only
 *  - second-page slicing (offset arithmetic) keeps DESC ordering
 *  - empty-string operatorLike / moduleCodeLike / operateType are all treated as "no filter"
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

    @Test
    fun findDetailById_returnsNull_whenAuditIdAbsent() {
        // Main rows without a detail row are routine (the aspect only writes details in the `after`
        // phase for web requests); callers must get null, not an exception.
        seed("ro-15", operator = "u-1") // no detailId → no detail row
        assertNull(readOnlyService.findDetailById("ro-15"))
        assertNull(readOnlyService.findDetailById("never-existed"))
    }

    @Test
    fun findById_mapsEveryColumnBackToVo() {
        val opTime = LocalDateTime.of(2026, 6, 1, 8, 30, 15)
        val entity = SysAuditLogVo().apply {
            id = "ro-full"
            entityId = "ent-1"
            operateTypeId = 4
            operateType = "删除"
            moduleId = 11
            moduleName = "字典模块-🦊"
            moduleCode = "SYS.DICT"
            description = "全字段映射回归"
            operator = "李四"
            operatorId = "op-9"
            operatorUserType = "STAFF"
            tenantId = "t-map"
            sourceTenantId = "src-map"
            subSysCode = "sys-map"
            operateTime = Date.from(opTime.atZone(java.time.ZoneId.systemDefault()).toInstant())
            operateIp = 167772161L
            operateIpDictCode = "ipv4"
            clientOs = "macOS"
            clientBrowser = "Safari"
            requestType = "PUT"
        }
        auditService.submit(SysAuditLogModel().apply { entities = mutableListOf(entity) })

        val vo = readOnlyService.findById("ro-full")

        assertNotNull(vo)
        assertEquals("ro-full", vo.id)
        assertEquals("ent-1", vo.entityId)
        assertEquals(4, vo.operateTypeId)
        assertEquals("删除", vo.operateType)
        assertEquals(11, vo.moduleId)
        assertEquals("字典模块-🦊", vo.moduleName)
        assertEquals("SYS.DICT", vo.moduleCode)
        assertEquals("全字段映射回归", vo.description)
        assertEquals("李四", vo.operator)
        assertEquals("op-9", vo.operatorId)
        assertEquals("STAFF", vo.operatorUserType)
        assertEquals("t-map", vo.tenantId)
        assertEquals("src-map", vo.sourceTenantId)
        assertEquals("sys-map", vo.subSysCode)
        assertEquals(167772161L, vo.operateIp)
        assertEquals("ipv4", vo.operateIpDictCode)
        assertEquals("macOS", vo.clientOs)
        assertEquals("Safari", vo.clientBrowser)
        assertEquals("PUT", vo.requestType)
        // LocalDateTime (DB) → java.util.Date (VO) must round-trip through the system zone unchanged
        assertEquals(
            Date.from(opTime.atZone(java.time.ZoneId.systemDefault()).toInstant()),
            vo.operateTime,
        )
    }

    @Test
    fun findDetailById_mapsEveryColumnBackToVo() {
        val entity = SysAuditLogVo().apply {
            id = "ro-dfull"
            operateTime = Date()
        }
        val detail = SysAuditDetailLogVo().apply {
            id = "rod-full"
            auditId = "ro-dfull"
            operateUrl = "/api/x?q=中文"
            stringParams = "a=1"
            objectParams = """{"deep":{"emoji":"🦊"}}"""
            requestReferer = "https://ref.example"
            requestFormData = "f=v"
            description = "detail 映射"
        }
        auditService.submit(SysAuditLogModel().apply {
            entities = mutableListOf(entity)
            sysAuditDetailLogs = mutableListOf(detail)
        })

        val vo = readOnlyService.findDetailById("ro-dfull")

        assertNotNull(vo)
        assertEquals("rod-full", vo.id)
        assertEquals("ro-dfull", vo.auditId)
        assertEquals("/api/x?q=中文", vo.operateUrl)
        assertEquals("a=1", vo.stringParams)
        assertEquals("""{"deep":{"emoji":"🦊"}}""", vo.objectParams)
        assertEquals("https://ref.example", vo.requestReferer)
        assertEquals("f=v", vo.requestFormData)
        assertEquals("detail 映射", vo.description)
    }

    @Test
    fun findById_toleratesNullOperateTimeColumn() {
        // Legacy or externally-inserted rows can carry NULL operate_time (the production write path
        // always defaults it, but retention imports may not). The row mapper must surface null, not NPE.
        TransactionTemplate(transactionManager).execute {
            val db = KudosContextHolder.currentDatabase()
            db.insert(SysAuditLogTable) {
                set(SysAuditLogTable.id, "ro-null-time")
            }
        }

        val vo = readOnlyService.findById("ro-null-time")

        assertNotNull(vo)
        assertNull(vo.operateTime, "NULL operate_time column must map to a null VO field")
    }

    @Test
    fun pagingSearch_filterBySourceTenantId() {
        seed("ro-16a", operator = "u-1", sourceTenant = "src-A")
        seed("ro-16b", operator = "u-2", sourceTenant = "src-B")

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { sourceTenantId = "src-A" }, pageNo = 1, pageSize = 10,
        )

        assertEquals(1L, page.total)
        assertEquals("ro-16a", page.items.single().id)
    }

    @Test
    fun pagingSearch_filterBySubSysCode() {
        seed("ro-17a", operator = "u-1", subSys = "sys-user")
        seed("ro-17b", operator = "u-2", subSys = "sys-auth")

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { subSysCode = "sys-auth" }, pageNo = 1, pageSize = 10,
        )

        assertEquals(1L, page.total)
        assertEquals("ro-17b", page.items.single().id)
    }

    @Test
    fun pagingSearch_moduleCode_exactMatchOnly() {
        seed("ro-18a", operator = "u-x", moduleCode = "user")
        seed("ro-18b", operator = "u-x", moduleCode = "user.account") // prefix sibling must NOT match exact filter

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { moduleCode = "user" }, pageNo = 1, pageSize = 10,
        )

        assertEquals(1L, page.total)
        assertEquals("ro-18a", page.items.single().id)
    }

    @Test
    fun pagingSearch_filterByOperatorUserType() {
        seed("ro-19a", operator = "u-1", operatorUserType = "ADMIN")
        seed("ro-19b", operator = "u-2", operatorUserType = "STAFF")

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { operatorUserType = "ADMIN" }, pageNo = 1, pageSize = 10,
        )

        assertEquals(1L, page.total)
        assertEquals("ro-19a", page.items.single().id)
    }

    @Test
    fun pagingSearch_filterByEntityId() {
        seed("ro-20a", operator = "u-1") // seed sets entityId = "entity-ro-20a"
        seed("ro-20b", operator = "u-2")

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { entityId = "entity-ro-20b" }, pageNo = 1, pageSize = 10,
        )

        assertEquals(1L, page.total)
        assertEquals("ro-20b", page.items.single().id)
    }

    @Test
    fun pagingSearch_operateTimeFromOnly_isInclusiveLowerBoundWithoutUpper() {
        val base = LocalDateTime.of(2026, 6, 2, 9, 0, 0)
        seed("ro-21a", operator = "u-x", operateTime = base.minusMinutes(1))
        seed("ro-21b", operator = "u-x", operateTime = base)
        seed("ro-21c", operator = "u-x", operateTime = base.plusYears(1)) // far future still matches (no upper bound)

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { operateTimeFrom = base }, pageNo = 1, pageSize = 10,
        )

        assertEquals(2L, page.total)
        assertEquals(setOf("ro-21b", "ro-21c"), page.items.map { it.id }.toSet())
    }

    @Test
    fun pagingSearch_operateTimeToOnly_isExclusiveUpperBoundWithoutLower() {
        val base = LocalDateTime.of(2026, 6, 2, 9, 0, 0)
        seed("ro-22a", operator = "u-x", operateTime = base.minusYears(1)) // distant past matches (no lower bound)
        seed("ro-22b", operator = "u-x", operateTime = base.minusSeconds(1))
        seed("ro-22c", operator = "u-x", operateTime = base) // exactly at upper bound → excluded

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { operateTimeTo = base }, pageNo = 1, pageSize = 10,
        )

        assertEquals(2L, page.total)
        assertEquals(setOf("ro-22a", "ro-22b"), page.items.map { it.id }.toSet())
    }

    @Test
    fun pagingSearch_secondPage_returnsNextSliceInDescOrder() {
        val t0 = LocalDateTime.of(2026, 6, 3, 10, 0, 0)
        // operate_time ascending with the id suffix, so DESC order is ro-23-5, ro-23-4, ..., ro-23-1
        (1..5).forEach { seed("ro-23-$it", operator = "u-x", operateTime = t0.plusMinutes(it.toLong())) }

        val page2 = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 2, pageSize = 2)

        assertEquals(5L, page2.total)
        assertEquals(2, page2.pageNo)
        assertEquals(
            listOf("ro-23-3", "ro-23-2"), page2.items.map { it.id },
            "Page 2 of size 2 must skip the 2 newest rows and keep DESC ordering",
        )

        val page3 = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 3, pageSize = 2)
        assertEquals(listOf("ro-23-1"), page3.items.map { it.id }, "Last page carries the remainder slice")
    }

    @Test
    fun pagingSearch_emptyStringOperatorLikeModuleCodeLikeAndOperateType_skipTheirFilters() {
        seed("ro-24", operator = "u-x", operatorName = "Nobody", moduleCode = "misc", operateTypeText = "noop")

        val query = AuditLogQuery().apply {
            operatorLike = ""
            moduleCodeLike = ""
            operateType = ""
        }
        val page = readOnlyService.pagingSearch(query, pageNo = 1, pageSize = 10)

        assertEquals(1L, page.total, "Empty-string fuzzy/exact text filters must behave as 'no filter', not LIKE '%%' or = ''")
    }

    // ----- Fixtures -----

    /**
     * Seeds a single audit-log main row (and optional detail). Sensible defaults so each test only
     * names the fields it actually cares about; named-arg call sites read like assertion specs.
     */
    private fun seed(
        id: String,
        tenant: String? = "t-default",
        sourceTenant: String? = null,
        subSys: String? = null,
        operator: String? = "u-default",
        operatorName: String? = null,
        operatorUserType: String? = null,
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
            this.operatorUserType = operatorUserType
            this.tenantId = tenant
            this.sourceTenantId = sourceTenant
            this.subSysCode = subSys
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
