package io.kudos.ms.msg.common.instance.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgInstanceEdit
 *
 * Covers defaults, full construction, IIdEntity contract and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgInstanceEditTest {

    @Test
    fun defaults() {
        val e = MsgInstanceEdit()
        assertEquals("", e.id)
        assertNull(e.localeDictCode)
        assertNull(e.title)
        assertNull(e.tenantId)
        assertTrue(e is IIdEntity<*>)
    }

    @Test
    fun fullConstruction() {
        val e = MsgInstanceEdit(
            id = "i-1", localeDictCode = "en", title = "t", content = "c", templateId = "tpl",
            sendTypeDictCode = "s", eventTypeDictCode = "e", msgTypeDictCode = "m",
            validTimeStart = null, validTimeEnd = null, tenantId = "tn",
        )
        assertEquals("i-1", e.id)
        assertEquals("c", e.content)
    }

    @Test
    fun dataClassContract() {
        val a = MsgInstanceEdit(id = "i-1", title = "t")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(title = "x"))
        assertTrue(a.toString().contains("i-1"))
    }

}
