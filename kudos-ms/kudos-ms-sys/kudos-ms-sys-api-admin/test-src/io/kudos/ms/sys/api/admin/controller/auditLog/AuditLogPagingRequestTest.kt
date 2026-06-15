package io.kudos.ms.sys.api.admin.controller.auditLog

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure unit test for [AuditLogPagingRequest]: the [AuditLogPagingRequest.toQuery] field mapping,
 * the lenient datetime parsing (UI format / ISO fallback / failure), data-class semantics and
 * default values. No Spring context required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AuditLogPagingRequestTest {

    @Test
    fun defaultsAreAllNull() {
        val req = AuditLogPagingRequest()
        assertNull(req.pageNo)
        assertNull(req.pageSize)
        assertNull(req.tenantId)
        assertNull(req.sourceTenantId)
        assertNull(req.subSysCode)
        assertNull(req.moduleCode)
        assertNull(req.moduleCodeLike)
        assertNull(req.operateTypeId)
        assertNull(req.operateType)
        assertNull(req.operatorId)
        assertNull(req.operatorLike)
        assertNull(req.operatorUserType)
        assertNull(req.entityId)
        assertNull(req.operateTimeStart)
        assertNull(req.operateTimeEnd)
        assertNull(req.descriptionLike)
        assertNull(req.orders)
    }

    @Test
    fun toQueryMapsEveryFilterField() {
        val req = AuditLogPagingRequest(
            pageNo = 3,
            pageSize = 25,
            tenantId = "t1",
            sourceTenantId = "st1",
            subSysCode = "user",
            moduleCode = "user.account",
            moduleCodeLike = "user",
            operateTypeId = 7,
            operateType = "CREATE",
            operatorId = "op-1",
            operatorLike = "alice",
            operatorUserType = "ADMIN",
            entityId = "e-9",
            descriptionLike = "login",
            orders = listOf(mapOf("property" to "operateTime")),
        )

        val q = req.toQuery()

        assertEquals("t1", q.tenantId)
        assertEquals("st1", q.sourceTenantId)
        assertEquals("user", q.subSysCode)
        assertEquals("user.account", q.moduleCode)
        assertEquals("user", q.moduleCodeLike)
        assertEquals(7, q.operateTypeId)
        assertEquals("CREATE", q.operateType)
        assertEquals("op-1", q.operatorId)
        assertEquals("alice", q.operatorLike)
        assertEquals("ADMIN", q.operatorUserType)
        assertEquals("e-9", q.entityId)
        assertEquals("login", q.descriptionLike)
    }

    @Test
    fun toQueryLeavesTimeBoundsNullWhenNotProvided() {
        val q = AuditLogPagingRequest().toQuery()
        assertNull(q.operateTimeFrom)
        assertNull(q.operateTimeTo)
    }

    @Test
    fun toQueryParsesUiDatetimeFormat() {
        val req = AuditLogPagingRequest(
            operateTimeStart = "2026-06-15 08:30:00",
            operateTimeEnd = "2026-06-15 18:45:59",
        )
        val q = req.toQuery()
        assertEquals(LocalDateTime.of(2026, 6, 15, 8, 30, 0), q.operateTimeFrom)
        assertEquals(LocalDateTime.of(2026, 6, 15, 18, 45, 59), q.operateTimeTo)
    }

    @Test
    fun toQueryParsesIsoDatetimeFallback() {
        val req = AuditLogPagingRequest(operateTimeStart = "2026-06-15T08:30:00")
        val q = req.toQuery()
        assertEquals(LocalDateTime.of(2026, 6, 15, 8, 30, 0), q.operateTimeFrom)
    }

    @Test
    fun toQueryTrimsAndTreatsBlankAsNull() {
        // Leading/trailing whitespace is trimmed before parsing.
        val req = AuditLogPagingRequest(operateTimeStart = "  2026-06-15 08:30:00  ")
        assertEquals(LocalDateTime.of(2026, 6, 15, 8, 30, 0), req.toQuery().operateTimeFrom)

        // Pure-blank string yields null (no filter).
        assertNull(AuditLogPagingRequest(operateTimeStart = "   ").toQuery().operateTimeFrom)
        assertNull(AuditLogPagingRequest(operateTimeStart = "").toQuery().operateTimeFrom)
    }

    @Test
    fun toQueryThrowsOnUnparseableDatetime() {
        val req = AuditLogPagingRequest(operateTimeStart = "15/06/2026")
        val ex = assertFailsWith<IllegalArgumentException> { req.toQuery() }
        assertTrue(ex.message!!.contains("Cannot parse datetime"))
        assertTrue(ex.message!!.contains("15/06/2026"))
    }

    @Test
    fun toQueryThrowsOnInvalidDatetimeValue() {
        // Right shape, impossible value (month 13) -> still a parse failure.
        val req = AuditLogPagingRequest(operateTimeEnd = "2026-13-40 08:30:00")
        assertFailsWith<IllegalArgumentException> { req.toQuery() }
    }

    @Test
    fun dataClassEqualityAndCopy() {
        val a = AuditLogPagingRequest(pageNo = 1, tenantId = "t")
        val b = AuditLogPagingRequest(pageNo = 1, tenantId = "t")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        val c = a.copy(pageNo = 2)
        assertEquals(2, c.pageNo)
        assertEquals("t", c.tenantId)
    }

    @Test
    fun mutablePropertiesCanBeReassigned() {
        val req = AuditLogPagingRequest()
        req.pageNo = 5
        req.tenantId = "tenant-x"
        assertEquals(5, req.pageNo)
        assertEquals("tenant-x", req.tenantId)
    }
}
