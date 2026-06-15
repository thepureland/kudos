package io.kudos.ms.msg.common.instance.vo.request

import io.kudos.ms.msg.common.instance.vo.response.MsgInstanceRow
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgInstanceQuery
 *
 * Covers all-null defaults, full-args construction, ListSearchPayload overrides
 * (getReturnEntityClass / isUnpagedSearchAllowed) and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgInstanceQueryTest {

    @Test
    fun defaultsAreAllNull() {
        val q = MsgInstanceQuery()
        assertNull(q.localeDictCode)
        assertNull(q.title)
        assertNull(q.content)
        assertNull(q.templateId)
        assertNull(q.sendTypeDictCode)
        assertNull(q.eventTypeDictCode)
        assertNull(q.msgTypeDictCode)
        assertNull(q.validTimeStart)
        assertNull(q.validTimeEnd)
        assertNull(q.tenantId)
    }

    @Test
    fun fullArgsConstruction() {
        val start = LocalDateTime.of(2026, 1, 1, 0, 0)
        val q = MsgInstanceQuery(
            localeDictCode = "en_US",
            title = "t",
            content = "c",
            templateId = "tpl",
            sendTypeDictCode = "s",
            eventTypeDictCode = "e",
            msgTypeDictCode = "m",
            validTimeStart = start,
            validTimeEnd = null,
            tenantId = "tn",
        )
        assertEquals("en_US", q.localeDictCode)
        assertEquals("t", q.title)
        assertEquals(start, q.validTimeStart)
        assertEquals("tn", q.tenantId)
    }

    @Test
    fun payloadOverrides() {
        val q = MsgInstanceQuery()
        assertEquals(MsgInstanceRow::class, q.getReturnEntityClass())
        assertFalse(q.isUnpagedSearchAllowed())
    }

    @Test
    fun dataClassContract() {
        val a = MsgInstanceQuery(title = "t")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(title = "x"))
        assertTrue(a.toString().contains("t"))
    }

}
