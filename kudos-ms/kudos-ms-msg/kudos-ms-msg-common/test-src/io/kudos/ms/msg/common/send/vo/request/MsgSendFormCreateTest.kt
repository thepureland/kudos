package io.kudos.ms.msg.common.send.vo.request

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgSendFormCreate
 *
 * Covers IMsgSendFormBase overrides (incl. Int counts), null handling and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSendFormCreateTest {

    @Test
    fun properties() {
        val ct = LocalDateTime.of(2026, 1, 1, 0, 0)
        val form: IMsgSendFormBase = MsgSendFormCreate(
            receiverGroupTypeDictCode = "dept",
            receiverGroupId = "g-1",
            instanceId = "i-1",
            msgTypeDictCode = "m",
            localeDictCode = "en",
            sendStatusDictCode = "00",
            createTime = ct,
            updateTime = null,
            successCount = 0,
            failCount = 0,
            jobId = "job-1",
            tenantId = "tn",
        )
        assertEquals("dept", form.receiverGroupTypeDictCode)
        assertEquals("g-1", form.receiverGroupId)
        assertEquals("i-1", form.instanceId)
        assertEquals("00", form.sendStatusDictCode)
        assertEquals(ct, form.createTime)
        assertEquals(0, form.successCount)
        assertEquals(0, form.failCount)
        assertEquals("job-1", form.jobId)
        assertEquals("tn", form.tenantId)
    }

    @Test
    fun allNull() {
        val form = MsgSendFormCreate(null, null, null, null, null, null, null, null, null, null, null, null)
        assertNull(form.receiverGroupId)
        assertNull(form.successCount)
        assertNull(form.tenantId)
    }

    @Test
    fun dataClassContract() {
        val a = MsgSendFormCreate("dept", "g-1", "i-1", "m", "en", "00", null, null, 1, 2, "job", "tn")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(failCount = 99))
        assertTrue(a.toString().contains("g-1"))
    }

}
