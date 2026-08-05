package io.kudos.ms.msg.common.send.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgSendEdit
 *
 * Covers defaults, full construction, IIdEntity contract and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSendEditTest {

    @Test
    fun defaults() {
        val e = MsgSendEdit()
        assertEquals("", e.id)
        assertNull(e.sendStatusDictCode)
        assertNull(e.successCount)
        assertNull(e.tenantId)
        assertTrue(e is IIdEntity<*>)
    }

    @Test
    fun fullConstruction() {
        val e = MsgSendEdit("s-1", "dept", "g-1", "i-1", "m", "en", "11", null, null, 2, 0, "job", "tn")
        assertEquals("s-1", e.id)
        assertEquals("11", e.sendStatusDictCode)
        assertEquals(2, e.successCount)
    }

    @Test
    fun dataClassContract() {
        val a = MsgSendEdit(id = "s-1", instanceId = "i-1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(instanceId = "i-2"))
        assertTrue(a.toString().contains("s-1"))
    }

}
