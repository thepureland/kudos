package io.kudos.ms.msg.common.receiver.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for MsgReceiverGroupFormUpdate
 *
 * Covers the required id (IIdEntity), IMsgReceiverGroupFormBase overrides and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiverGroupFormUpdateTest {

    @Test
    fun properties() {
        val form = MsgReceiverGroupFormUpdate(
            id = "g-1",
            receiverGroupTypeDictCode = "dept",
            defineTable = "tbl",
            nameColumn = "name",
            remark = "r",
            active = false,
        )
        assertEquals("g-1", form.id)
        assertEquals("dept", form.receiverGroupTypeDictCode)
        assertEquals(false, form.active)
        assertTrue(form is IIdEntity<*>)
        assertTrue(form is IMsgReceiverGroupFormBase)
    }

    @Test
    fun dataClassContract() {
        val a = MsgReceiverGroupFormUpdate("g-1", "dept", "tbl", "name", "r", true)
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "g-2"))
        assertTrue(a.toString().contains("g-1"))
    }

}
