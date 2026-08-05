package io.kudos.ms.msg.common.send.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgSendRow
 *
 * Covers defaults, full construction, IIdEntity contract and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSendRowTest {

    @Test
    fun defaults() {
        val r = MsgSendRow()
        assertEquals("", r.id)
        assertNull(r.receiverGroupId)
        assertNull(r.failCount)
        assertNull(r.jobId)
        assertTrue(r is IIdEntity<*>)
    }

    @Test
    fun fullConstruction() {
        val r = MsgSendRow("s-1", "dept", "g-1", "i-1", "m", "en", "33", null, null, 9, 1, "job", "tn")
        assertEquals("s-1", r.id)
        assertEquals("job", r.jobId)
        assertEquals(1, r.failCount)
    }

    @Test
    fun dataClassContract() {
        val a = MsgSendRow(id = "s-1", instanceId = "i-1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "s-2"))
        assertTrue(a.toString().contains("s-1"))
    }

}
