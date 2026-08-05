package io.kudos.ability.log.audit.common.entity

import java.io.Serializable
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [AuditLogQuery].
 *
 * Coverage:
 *  - Every filter field defaults to null ("no filter on this dimension" contract).
 *  - Every filter field round-trips through its setter/getter, including time bounds.
 *  - The query object is [Serializable] (cross-process forwarding contract).
 *
 * @author K
 * @since 1.0.0
 */
internal class AuditLogQueryTest {

    @Test
    fun allFields_defaultToNull() {
        val q = AuditLogQuery()
        assertNull(q.tenantId)
        assertNull(q.sourceTenantId)
        assertNull(q.subSysCode)
        assertNull(q.moduleCode)
        assertNull(q.operateTypeId)
        assertNull(q.operatorId)
        assertNull(q.operatorUserType)
        assertNull(q.operatorLike)
        assertNull(q.moduleCodeLike)
        assertNull(q.operateType)
        assertNull(q.entityId)
        assertNull(q.operateTimeFrom)
        assertNull(q.operateTimeTo)
        assertNull(q.descriptionLike)
    }

    @Test
    fun allFields_roundTrip() {
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 12, 31, 23, 59)
        val q = AuditLogQuery().apply {
            tenantId = "t-1"
            sourceTenantId = "src-t"
            subSysCode = "user"
            moduleCode = "user.account"
            operateTypeId = 3
            operatorId = "op-9"
            operatorUserType = "ADMIN"
            operatorLike = "ali"
            moduleCodeLike = "user"
            operateType = "update"
            entityId = "e-42"
            operateTimeFrom = from
            operateTimeTo = to
            descriptionLike = "登入"
        }

        assertEquals("t-1", q.tenantId)
        assertEquals("src-t", q.sourceTenantId)
        assertEquals("user", q.subSysCode)
        assertEquals("user.account", q.moduleCode)
        assertEquals(3, q.operateTypeId)
        assertEquals("op-9", q.operatorId)
        assertEquals("ADMIN", q.operatorUserType)
        assertEquals("ali", q.operatorLike)
        assertEquals("user", q.moduleCodeLike)
        assertEquals("update", q.operateType)
        assertEquals("e-42", q.entityId)
        assertEquals(from, q.operateTimeFrom)
        assertEquals(to, q.operateTimeTo)
        assertEquals("登入", q.descriptionLike)
    }

    @Test
    fun query_isSerializable() {
        assertTrue(AuditLogQuery() is Serializable)
    }
}
