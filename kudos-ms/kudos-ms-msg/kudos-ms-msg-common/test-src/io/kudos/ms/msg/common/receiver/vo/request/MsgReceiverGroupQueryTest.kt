package io.kudos.ms.msg.common.receiver.vo.request

import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiverGroupRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgReceiverGroupQuery
 *
 * Covers all-null defaults, full construction, ListSearchPayload overrides and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiverGroupQueryTest {

    @Test
    fun defaultsAreAllNull() {
        val q = MsgReceiverGroupQuery()
        assertNull(q.receiverGroupTypeDictCode)
        assertNull(q.defineTable)
        assertNull(q.nameColumn)
        assertNull(q.remark)
        assertNull(q.active)
        assertNull(q.builtIn)
    }

    @Test
    fun fullArgsConstruction() {
        val q = MsgReceiverGroupQuery("dept", "tbl", "name", "r", true, false)
        assertEquals("dept", q.receiverGroupTypeDictCode)
        assertEquals("tbl", q.defineTable)
        assertEquals(true, q.active)
        assertEquals(false, q.builtIn)
    }

    @Test
    fun payloadOverrides() {
        val q = MsgReceiverGroupQuery()
        assertEquals(MsgReceiverGroupRow::class, q.getReturnEntityClass())
        assertFalse(q.isUnpagedSearchAllowed())
    }

    @Test
    fun dataClassContract() {
        val a = MsgReceiverGroupQuery(receiverGroupTypeDictCode = "dept")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = true))
        assertTrue(a.toString().contains("dept"))
    }

}
