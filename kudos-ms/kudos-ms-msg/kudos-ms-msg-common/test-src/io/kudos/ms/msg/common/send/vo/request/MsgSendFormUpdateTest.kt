package io.kudos.ms.msg.common.send.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for MsgSendFormUpdate
 *
 * Covers the required id (IIdEntity), IMsgSendFormBase overrides and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSendFormUpdateTest {

    @Test
    fun properties() {
        val form = MsgSendFormUpdate(
            id = "s-1",
            receiverGroupTypeDictCode = "dept",
            receiverGroupId = "g-1",
            instanceId = "i-1",
            msgTypeDictCode = "m",
            localeDictCode = "en",
            sendStatusDictCode = "33",
            createTime = null,
            updateTime = null,
            successCount = 5,
            failCount = 1,
            jobId = "job",
            tenantId = "tn",
        )
        assertEquals("s-1", form.id)
        assertEquals("33", form.sendStatusDictCode)
        assertEquals(5, form.successCount)
        assertTrue(form is IIdEntity<*>)
        assertTrue(form is IMsgSendFormBase)
    }

    @Test
    fun dataClassContract() {
        val a = MsgSendFormUpdate("s-1", "dept", "g-1", "i-1", "m", "en", "33", null, null, 5, 1, "job", "tn")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "s-2"))
        assertTrue(a.toString().contains("s-1"))
    }

}
