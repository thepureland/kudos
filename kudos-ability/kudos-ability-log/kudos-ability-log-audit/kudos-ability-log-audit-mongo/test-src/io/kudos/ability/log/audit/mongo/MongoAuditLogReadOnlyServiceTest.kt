package io.kudos.ability.log.audit.mongo

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import io.kudos.ability.log.audit.common.api.IAuditLogReadOnlyService
import io.kudos.ability.log.audit.common.entity.AuditLogQuery
import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import io.kudos.ability.log.audit.mongo.entity.SysAuditDetailLogDocument
import io.kudos.ability.log.audit.mongo.entity.SysAuditLogDocument
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.MongoTestContainer
import jakarta.annotation.Resource
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Integration tests for [MongoAuditLogReadOnlyService] against a real Mongo 7 testcontainer.
 *
 * Coverage:
 *  - `findById` returns the matching doc as a [SysAuditLogVo], null when absent.
 *  - `findDetailById` reads the **embedded** detail (no separate collection) — null when the
 *    main doc carries no detail, null when the auditId itself doesn't exist.
 *  - `pagingSearch` handles: filter combinations (tenantId / operatorId / time bounds /
 *    descriptionLike / operatorLike), empty filter (all rows), pagination (pageNo+pageSize),
 *    sort (operate_time DESC by default), regex escaping (descriptionLike with `.` doesn't act
 *    as a wildcard), and the empty-result short-circuit (no count → empty page).
 *
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
@Import(MongoAuditLogReadOnlyServiceTest.TestMongoClientConfig::class)
internal class MongoAuditLogReadOnlyServiceTest {

    @Resource
    private lateinit var readOnlyService: IAuditLogReadOnlyService

    @Resource
    private lateinit var mongoTemplate: MongoTemplate

    @BeforeTest
    fun cleanUp() {
        mongoTemplate.dropCollection(SysAuditLogDocument::class.java)
    }

    @AfterTest
    fun cleanUpAfter() {
        mongoTemplate.dropCollection(SysAuditLogDocument::class.java)
    }

    @Test
    fun findById_returnsVoWhenDocExists() {
        val doc = newDoc(id = "audit-1", entityId = "u-100", description = "create user").also { d ->
            d.detail = newDetail(id = "d-1", auditId = "audit-1", operateUrl = "/users")
        }
        mongoTemplate.save(doc)

        val vo: SysAuditLogVo = assertNotNull(readOnlyService.findById("audit-1"))
        assertEquals("audit-1", vo.id)
        assertEquals("u-100", vo.entityId)
        assertEquals("create user", vo.description)
    }

    @Test
    fun findById_returnsNullForMissing() {
        assertNull(readOnlyService.findById("does-not-exist"))
    }

    @Test
    fun findDetailById_returnsEmbeddedDetailAsVo() {
        // The embedded detail's auditId field is matched indirectly via the parent's `id`:
        // findDetailById(auditId) returns parent.detail. This mirrors the ktorm impl's contract.
        val doc = newDoc(id = "audit-2", entityId = "u-200").also { d ->
            d.detail = newDetail(id = "d-2", auditId = "audit-2", operateUrl = "/users/200", description = "DETAIL")
        }
        mongoTemplate.save(doc)

        val detailVo: SysAuditDetailLogVo = assertNotNull(readOnlyService.findDetailById("audit-2"))
        assertEquals("d-2", detailVo.id)
        assertEquals("audit-2", detailVo.auditId)
        assertEquals("/users/200", detailVo.operateUrl)
        assertEquals("DETAIL", detailVo.description)
    }

    @Test
    fun findDetailById_returnsNullWhenParentHasNoDetail() {
        // Audits without a detail are allowed (audit aspect on a method that doesn't pass detail
        // info, or backfilled audit rows). The lookup must distinguish "audit exists but detail
        // null" from "audit doesn't exist at all" but both surface as null per contract.
        val doc = newDoc(id = "no-detail-audit")
        mongoTemplate.save(doc) // detail = null by default
        assertNull(readOnlyService.findDetailById("no-detail-audit"))
    }

    @Test
    fun findDetailById_returnsNullWhenAuditDoesNotExist() {
        assertNull(readOnlyService.findDetailById("missing-audit"))
    }

    @Test
    fun pagingSearch_emptyCollection_returnsEmptyPage() {
        val page = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 1, pageSize = 10)
        assertEquals(0L, page.total)
        assertEquals(emptyList(), page.items)
        assertEquals(1, page.pageNo)
        assertEquals(10, page.pageSize)
    }

    @Test
    fun pagingSearch_noFilters_returnsAllRowsSortedByOperateTimeDesc() {
        val baseTime = LocalDateTime.of(2026, 1, 1, 10, 0)
        // Three rows with operateTime: 10:00, 11:00, 12:00 — DESC order should give us 12 / 11 / 10.
        mongoTemplate.save(newDoc(id = "earliest", operateTime = baseTime.toDate()))
        mongoTemplate.save(newDoc(id = "middle", operateTime = baseTime.plusHours(1).toDate()))
        mongoTemplate.save(newDoc(id = "latest", operateTime = baseTime.plusHours(2).toDate()))

        val page = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 1, pageSize = 10)

        assertEquals(3L, page.total)
        assertEquals(listOf("latest", "middle", "earliest"), page.items.map { it.id })
    }

    @Test
    fun pagingSearch_tenantId_filtersExact() {
        mongoTemplate.save(newDoc(id = "t-a-1", tenantId = "tenant-A"))
        mongoTemplate.save(newDoc(id = "t-a-2", tenantId = "tenant-A"))
        mongoTemplate.save(newDoc(id = "t-b-1", tenantId = "tenant-B"))

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { tenantId = "tenant-A" }, pageNo = 1, pageSize = 10
        )

        assertEquals(2L, page.total)
        assertEquals(setOf("t-a-1", "t-a-2"), page.items.map { it.id }.toSet())
    }

    @Test
    fun pagingSearch_operatorLike_isSubstringNotRegex() {
        // Verify that `.` in the input is escaped — must match the literal `.`, not "any char".
        mongoTemplate.save(newDoc(id = "lit-dot", operator = "abc.def"))
        mongoTemplate.save(newDoc(id = "should-not-match", operator = "abcXdef"))

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { operatorLike = "c.d" }, pageNo = 1, pageSize = 10
        )

        assertEquals(1L, page.total, "the `.` in the filter must match the literal `.`, not act as a wildcard")
        assertEquals("lit-dot", page.items.single().id)
    }

    @Test
    fun pagingSearch_descriptionLike_isCaseInsensitive() {
        mongoTemplate.save(newDoc(id = "upper", description = "UPDATE Order"))
        mongoTemplate.save(newDoc(id = "lower", description = "update order"))
        mongoTemplate.save(newDoc(id = "other", description = "create order"))

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { descriptionLike = "update" }, pageNo = 1, pageSize = 10
        )

        assertEquals(2L, page.total)
        assertEquals(setOf("upper", "lower"), page.items.map { it.id }.toSet())
    }

    @Test
    fun pagingSearch_descriptionLikeEmptyString_isTreatedAsNoFilter() {
        // Matches the ktorm impl's "empty string skips the filter" — otherwise an empty UI input
        // would translate to a LIKE '%%' (all rows) which is what we want, but via a special-case
        // path that doesn't pay the regex compilation cost.
        mongoTemplate.save(newDoc(id = "any-1"))
        mongoTemplate.save(newDoc(id = "any-2"))

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply { descriptionLike = "" }, pageNo = 1, pageSize = 10
        )

        assertEquals(2L, page.total, "empty descriptionLike means no filter; all rows must come back")
    }

    @Test
    fun pagingSearch_operateTimeWindow_inclusiveLowerExclusiveUpper() {
        // Three rows at 09:00, 10:00, 11:00; window [10:00, 11:00) → only the 10:00 row.
        val t9 = LocalDateTime.of(2026, 1, 1, 9, 0)
        val t10 = LocalDateTime.of(2026, 1, 1, 10, 0)
        val t11 = LocalDateTime.of(2026, 1, 1, 11, 0)
        mongoTemplate.save(newDoc(id = "at-9", operateTime = t9.toDate()))
        mongoTemplate.save(newDoc(id = "at-10", operateTime = t10.toDate()))
        mongoTemplate.save(newDoc(id = "at-11", operateTime = t11.toDate()))

        val page = readOnlyService.pagingSearch(
            AuditLogQuery().apply {
                operateTimeFrom = t10
                operateTimeTo = t11
            },
            pageNo = 1, pageSize = 10,
        )

        assertEquals(1L, page.total)
        assertEquals("at-10", page.items.single().id, "inclusive lower bound at 10:00, exclusive upper bound at 11:00")
    }

    @Test
    fun pagingSearch_pageNumberAndSize_slicesCorrectly() {
        // 7 rows with sequential operateTime; pageSize=3, page 1 → 3 newest, page 2 → next 3,
        // page 3 → 1 leftover.
        val base = LocalDateTime.of(2026, 1, 1, 10, 0)
        repeat(7) { i ->
            mongoTemplate.save(newDoc(id = "r-$i", operateTime = base.plusMinutes(i.toLong()).toDate()))
        }

        val page1 = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 1, pageSize = 3)
        val page2 = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 2, pageSize = 3)
        val page3 = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 3, pageSize = 3)

        assertEquals(7L, page1.total, "total reflects the unsliced match count")
        assertEquals(listOf("r-6", "r-5", "r-4"), page1.items.map { it.id })
        assertEquals(listOf("r-3", "r-2", "r-1"), page2.items.map { it.id })
        assertEquals(listOf("r-0"), page3.items.map { it.id })
    }

    @Test
    fun pagingSearch_pageNoOrSizeBelowOne_areClampedToOne() {
        mongoTemplate.save(newDoc(id = "only-one"))
        val page = readOnlyService.pagingSearch(AuditLogQuery(), pageNo = 0, pageSize = -5)
        assertEquals(1, page.pageNo, "pageNo < 1 must clamp to 1")
        assertEquals(1, page.pageSize, "pageSize < 1 must clamp to 1")
        assertEquals(1L, page.total)
        assertEquals("only-one", page.items.single().id)
    }

    @Test
    fun pagingSearch_combinedFilters_andCombined() {
        // tenantId == "T" AND operatorId == "alice" AND descriptionLike "fast"
        mongoTemplate.save(newDoc(id = "match", tenantId = "T", operatorId = "alice", description = "do something fast"))
        mongoTemplate.save(newDoc(id = "wrong-tenant", tenantId = "X", operatorId = "alice", description = "fast op"))
        mongoTemplate.save(newDoc(id = "wrong-op", tenantId = "T", operatorId = "bob", description = "fast op"))
        mongoTemplate.save(newDoc(id = "wrong-desc", tenantId = "T", operatorId = "alice", description = "slow"))

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

    private fun newDoc(
        id: String,
        entityId: String? = null,
        description: String? = null,
        operator: String? = null,
        operatorId: String? = null,
        tenantId: String? = null,
        operateTime: Date? = Date(),
    ): SysAuditLogDocument = SysAuditLogDocument().also { d ->
        d.id = id
        d.entityId = entityId
        d.description = description
        d.operator = operator
        d.operatorId = operatorId
        d.tenantId = tenantId
        d.operateTime = operateTime
    }

    private fun newDetail(
        id: String,
        auditId: String,
        operateUrl: String? = null,
        description: String? = null,
    ): SysAuditDetailLogDocument = SysAuditDetailLogDocument().also { d ->
        d.id = id
        d.auditId = auditId
        d.operateUrl = operateUrl
        d.description = description
    }

    private fun LocalDateTime.toDate(): Date =
        Date.from(this.atZone(ZoneId.systemDefault()).toInstant())

    @TestConfiguration(proxyBeanMethods = false)
    open class TestMongoClientConfig {
        @Bean
        @Primary
        open fun testMongoClient(environment: Environment): MongoClient {
            val uri = requireNotNull(environment.getProperty("spring.data.mongodb.uri")) {
                "spring.data.mongodb.uri was not registered; MongoTestContainer.startIfNeeded() should have set it"
            }
            return MongoClients.create(uri)
        }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry?) {
            MongoTestContainer.startIfNeeded(registry)
        }
    }
}
