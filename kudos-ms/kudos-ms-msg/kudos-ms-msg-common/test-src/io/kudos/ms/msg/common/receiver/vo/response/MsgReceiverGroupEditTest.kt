package io.kudos.ms.msg.common.receiver.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgReceiverGroupEdit
 *
 * Covers defaults, full construction, IIdEntity contract and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiverGroupEditTest {

    @Test
    fun defaults() {
        val e = MsgReceiverGroupEdit()
        assertEquals("", e.id)
        assertNull(e.receiverGroupTypeDictCode)
        assertNull(e.defineTable)
        assertNull(e.active)
        assertTrue(e is IIdEntity<*>)
    }

    @Test
    fun fullConstruction() {
        val e = MsgReceiverGroupEdit("g-1", "dept", "tbl", "name", "r", true)
        assertEquals("g-1", e.id)
        assertEquals("dept", e.receiverGroupTypeDictCode)
        assertEquals(true, e.active)
    }

    @Test
    fun dataClassContract() {
        val a = MsgReceiverGroupEdit(id = "g-1", remark = "r")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = false))
        assertTrue(a.toString().contains("g-1"))
    }

}
