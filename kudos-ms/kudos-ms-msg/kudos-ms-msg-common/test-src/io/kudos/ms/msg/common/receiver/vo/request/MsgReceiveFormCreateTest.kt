package io.kudos.ms.msg.common.receiver.vo.request

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgReceiveFormCreate
 *
 * Covers IMsgReceiveFormBase overrides, null handling and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiveFormCreateTest {

    @Test
    fun properties() {
        val ct = LocalDateTime.of(2026, 1, 1, 0, 0)
        val form: IMsgReceiveFormBase = MsgReceiveFormCreate(
            receiverId = "u-1",
            sendId = "s-1",
            receiveStatusDictCode = "11",
            createTime = ct,
            updateTime = null,
            tenantId = "tn",
        )
        assertEquals("u-1", form.receiverId)
        assertEquals("s-1", form.sendId)
        assertEquals("11", form.receiveStatusDictCode)
        assertEquals(ct, form.createTime)
        assertNull(form.updateTime)
        assertEquals("tn", form.tenantId)
    }

    @Test
    fun allNull() {
        val form = MsgReceiveFormCreate(null, null, null, null, null, null)
        assertNull(form.receiverId)
        assertNull(form.sendId)
        assertNull(form.tenantId)
    }

    @Test
    fun dataClassContract() {
        val a = MsgReceiveFormCreate("u-1", "s-1", "11", null, null, "tn")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(receiverId = "u-2"))
        assertTrue(a.toString().contains("u-1"))
    }

}
