package io.kudos.ms.msg.common.receiver.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for MsgReceiveFormUpdate
 *
 * Covers the required id (IIdEntity), IMsgReceiveFormBase overrides and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiveFormUpdateTest {

    @Test
    fun properties() {
        val ut = LocalDateTime.of(2026, 2, 2, 0, 0)
        val form = MsgReceiveFormUpdate(
            id = "r-1",
            receiverId = "u-1",
            sendId = "s-1",
            receiveStatusDictCode = "12",
            createTime = null,
            updateTime = ut,
            tenantId = "tn",
        )
        assertEquals("r-1", form.id)
        assertEquals("u-1", form.receiverId)
        assertEquals("12", form.receiveStatusDictCode)
        assertEquals(ut, form.updateTime)
        assertTrue(form is IIdEntity<*>)
        assertTrue(form is IMsgReceiveFormBase)
    }

    @Test
    fun dataClassContract() {
        val a = MsgReceiveFormUpdate("r-1", "u-1", "s-1", "12", null, null, "tn")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "r-2"))
        assertTrue(a.toString().contains("r-1"))
    }

}
