package io.kudos.ms.msg.common.receiver.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgReceiveEdit
 *
 * Covers defaults, full construction, IIdEntity contract and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiveEditTest {

    @Test
    fun defaults() {
        val e = MsgReceiveEdit()
        assertEquals("", e.id)
        assertNull(e.receiverId)
        assertNull(e.receiveStatusDictCode)
        assertNull(e.tenantId)
        assertTrue(e is IIdEntity<*>)
    }

    @Test
    fun fullConstruction() {
        val e = MsgReceiveEdit("r-1", "u-1", "s-1", "12", null, null, "tn")
        assertEquals("r-1", e.id)
        assertEquals("12", e.receiveStatusDictCode)
    }

    @Test
    fun dataClassContract() {
        val a = MsgReceiveEdit(id = "r-1", receiverId = "u-1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(receiverId = "u-2"))
        assertTrue(a.toString().contains("r-1"))
    }

}
