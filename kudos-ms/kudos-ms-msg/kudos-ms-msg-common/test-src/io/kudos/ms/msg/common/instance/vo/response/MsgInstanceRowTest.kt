package io.kudos.ms.msg.common.instance.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgInstanceRow
 *
 * Covers defaults, full construction, IIdEntity contract and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgInstanceRowTest {

    @Test
    fun defaults() {
        val r = MsgInstanceRow()
        assertEquals("", r.id)
        assertNull(r.localeDictCode)
        assertNull(r.title)
        assertNull(r.tenantId)
        assertTrue(r is IIdEntity<*>)
    }

    @Test
    fun fullConstruction() {
        val r = MsgInstanceRow(
            id = "i-1", localeDictCode = "en", title = "t", content = "c", templateId = "tpl",
            sendTypeDictCode = "s", eventTypeDictCode = "e", msgTypeDictCode = "m",
            validTimeStart = null, validTimeEnd = null, tenantId = "tn",
        )
        assertEquals("i-1", r.id)
        assertEquals("e", r.eventTypeDictCode)
    }

    @Test
    fun dataClassContract() {
        val a = MsgInstanceRow(id = "i-1", title = "t")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(tenantId = "tn-2"))
        assertTrue(a.toString().contains("i-1"))
    }

}
