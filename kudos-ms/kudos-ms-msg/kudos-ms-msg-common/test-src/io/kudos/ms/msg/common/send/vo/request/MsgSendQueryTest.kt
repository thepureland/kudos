package io.kudos.ms.msg.common.send.vo.request

import io.kudos.ms.msg.common.send.vo.response.MsgSendRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgSendQuery
 *
 * Covers all-null defaults, full construction, ListSearchPayload overrides and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSendQueryTest {

    @Test
    fun defaultsAreAllNull() {
        val q = MsgSendQuery()
        assertNull(q.receiverGroupTypeDictCode)
        assertNull(q.receiverGroupId)
        assertNull(q.instanceId)
        assertNull(q.msgTypeDictCode)
        assertNull(q.localeDictCode)
        assertNull(q.sendStatusDictCode)
        assertNull(q.createTime)
        assertNull(q.updateTime)
        assertNull(q.successCount)
        assertNull(q.failCount)
        assertNull(q.jobId)
        assertNull(q.tenantId)
    }

    @Test
    fun fullArgsConstruction() {
        val q = MsgSendQuery(
            receiverGroupTypeDictCode = "dept", receiverGroupId = "g-1", instanceId = "i-1",
            msgTypeDictCode = "m", localeDictCode = "en", sendStatusDictCode = "11",
            createTime = null, updateTime = null, successCount = 3, failCount = 1, jobId = "job", tenantId = "tn",
        )
        assertEquals("dept", q.receiverGroupTypeDictCode)
        assertEquals("i-1", q.instanceId)
        assertEquals(3, q.successCount)
        assertEquals("tn", q.tenantId)
    }

    @Test
    fun payloadOverrides() {
        val q = MsgSendQuery()
        assertEquals(MsgSendRow::class, q.getReturnEntityClass())
        assertFalse(q.isUnpagedSearchAllowed())
    }

    @Test
    fun dataClassContract() {
        val a = MsgSendQuery(receiverGroupId = "g-1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(failCount = 9))
        assertTrue(a.toString().contains("g-1"))
    }

}
