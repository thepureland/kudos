package io.kudos.ms.msg.common.receiver.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgReceiverGroupRow
 *
 * Covers defaults, full construction, IIdEntity contract and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiverGroupRowTest {

    @Test
    fun defaults() {
        val r = MsgReceiverGroupRow()
        assertEquals("", r.id)
        assertNull(r.receiverGroupTypeDictCode)
        assertNull(r.active)
        assertNull(r.builtIn)
        assertTrue(r is IIdEntity<*>)
    }

    @Test
    fun fullConstruction() {
        val r = MsgReceiverGroupRow("g-1", "dept", "tbl", "name", "r", true, false)
        assertEquals("g-1", r.id)
        assertEquals("name", r.nameColumn)
        assertEquals(false, r.builtIn)
    }

    @Test
    fun dataClassContract() {
        val a = MsgReceiverGroupRow(id = "g-1", remark = "r")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(builtIn = true))
        assertTrue(a.toString().contains("g-1"))
    }

}
