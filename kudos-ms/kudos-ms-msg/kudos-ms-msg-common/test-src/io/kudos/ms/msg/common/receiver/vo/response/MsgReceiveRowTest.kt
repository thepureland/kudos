package io.kudos.ms.msg.common.receiver.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgReceiveRow
 *
 * Covers defaults, full construction, IIdEntity contract and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiveRowTest {

    @Test
    fun defaults() {
        val r = MsgReceiveRow()
        assertEquals("", r.id)
        assertNull(r.receiverId)
        assertNull(r.sendId)
        assertNull(r.tenantId)
        assertTrue(r is IIdEntity<*>)
    }

    @Test
    fun fullConstruction() {
        val r = MsgReceiveRow("r-1", "u-1", "s-1", "11", null, null, "tn")
        assertEquals("r-1", r.id)
        assertEquals("s-1", r.sendId)
    }

    @Test
    fun dataClassContract() {
        val a = MsgReceiveRow(id = "r-1", receiverId = "u-1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "r-2"))
        assertTrue(a.toString().contains("r-1"))
    }

}
