package io.kudos.ms.sys.api.admin.controller.auditLog

import io.kudos.ability.log.audit.common.api.IAuditLogReadOnlyService
import io.kudos.ability.log.audit.common.entity.AuditLogPage
import io.kudos.ability.log.audit.common.entity.AuditLogQuery
import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import io.kudos.base.error.ObjectNotFoundException
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [SysAuditLogAdminController]: paging defaults, the upper-bound page-size cap,
 * `Long -> Int` total narrowing, filter forwarding, and the getDetail found / not-found paths. The
 * read-only service is mocked and injected via reflection — no Spring context.
 *
 * Paging tests record the args the controller forwards via an [org.mockito.stubbing.Answer] rather
 * than Mockito captors, because a captor returns `null` for the non-null Kotlin `AuditLogQuery`
 * parameter and would trip the call-site null-check before Mockito sees it.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysAuditLogAdminControllerTest {

    private val service = mock(IAuditLogReadOnlyService::class.java)
    private val controller = SysAuditLogAdminController().also {
        ReflectionTestUtils.setField(it, "auditLogReadOnly", service)
    }

    private var capturedQuery: AuditLogQuery? = null
    private var capturedPageNo: Int? = null
    private var capturedPageSize: Int? = null

    private fun page(items: List<SysAuditLogVo>, total: Long) =
        AuditLogPage(items = items, total = total, pageNo = 1, pageSize = 10)

    init {
        // One global answer captures every pagingSearch invocation regardless of args.
        whenCalled(service.pagingSearch(anyQueryMatcher(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
            .thenAnswer { inv ->
                capturedQuery = inv.getArgument(0)
                capturedPageNo = inv.getArgument(1)
                capturedPageSize = inv.getArgument(2)
                pagingResult
            }
    }

    /** Result returned by the global pagingSearch answer; reassigned per-test. */
    private var pagingResult: AuditLogPage = AuditLogPage.empty(1, 10)

    /** Matcher that returns a non-null instance so the Kotlin call-site null-check passes. */
    private fun anyQueryMatcher(): AuditLogQuery {
        org.mockito.ArgumentMatchers.any(AuditLogQuery::class.java)
        return AuditLogQuery()
    }

    @Test
    fun pagingSearchAppliesDefaultPageNoAndPageSize() {
        pagingResult = page(emptyList(), 0)

        val result = controller.pagingSearch(AuditLogPagingRequest())

        assertEquals(0, result.totalCount)
        assertEquals(emptyList(), result.data)
        assertEquals(1, capturedPageNo)
        assertEquals(10, capturedPageSize)
    }

    @Test
    fun pagingSearchHonorsExplicitPageNoAndPageSize() {
        pagingResult = page(emptyList(), 7)

        val result = controller.pagingSearch(AuditLogPagingRequest(pageNo = 4, pageSize = 50))
        assertEquals(7, result.totalCount)
        assertEquals(4, capturedPageNo)
        assertEquals(50, capturedPageSize)
    }

    @Test
    fun pagingSearchCapsPageSizeAtUpperBound() {
        // 100 is MAX_PAGE_SIZE; any larger request must be clamped down to 100.
        pagingResult = page(emptyList(), 0)

        controller.pagingSearch(AuditLogPagingRequest(pageSize = 5000))
        assertEquals(100, capturedPageSize)
    }

    @Test
    fun pagingSearchKeepsPageSizeAtExactBoundary() {
        pagingResult = page(emptyList(), 0)

        controller.pagingSearch(AuditLogPagingRequest(pageSize = 100))
        assertEquals(100, capturedPageSize)
    }

    @Test
    fun pagingSearchForwardsFiltersIntoQuery() {
        pagingResult = page(emptyList(), 0)

        controller.pagingSearch(AuditLogPagingRequest(tenantId = "t-9", operatorLike = "bob"))

        assertEquals("t-9", capturedQuery!!.tenantId)
        assertEquals("bob", capturedQuery!!.operatorLike)
    }

    @Test
    fun pagingSearchNarrowsLongTotalToInt() {
        val item = SysAuditLogVo().apply { id = "x" }
        pagingResult = page(listOf(item), 42L)

        val result = controller.pagingSearch(AuditLogPagingRequest())
        assertEquals(42, result.totalCount)
        assertEquals(listOf(item), result.data)
    }

    @Test
    fun getDetailReturnsMergedDtoWhenMainExists() {
        val main = SysAuditLogVo().apply { id = "log-1"; description = "main" }
        val detail = SysAuditDetailLogVo().apply { operateUrl = "/u" }
        whenCalled(service.findById("log-1")).thenReturn(main)
        whenCalled(service.findDetailById("log-1")).thenReturn(detail)

        val dto = controller.getDetail("log-1")
        assertEquals("log-1", dto.id)
        assertEquals("main", dto.description)
        assertEquals("/u", dto.operateUrl)
        verify(service).findById("log-1")
        verify(service).findDetailById("log-1")
    }

    @Test
    fun getDetailWithMissingDetailRowStillReturnsDto() {
        val main = SysAuditLogVo().apply { id = "log-2" }
        whenCalled(service.findById("log-2")).thenReturn(main)
        whenCalled(service.findDetailById("log-2")).thenReturn(null)

        val dto = controller.getDetail("log-2")
        assertEquals("log-2", dto.id)
        assertNull(dto.operateUrl)
    }

    @Test
    fun getDetailThrowsWhenMainMissing() {
        whenCalled(service.findById("nope")).thenReturn(null)

        val ex = assertFailsWith<ObjectNotFoundException> { controller.getDetail("nope") }
        assertEquals("Audit log not found: nope", ex.message)
        verify(service).findById("nope")
    }
}
