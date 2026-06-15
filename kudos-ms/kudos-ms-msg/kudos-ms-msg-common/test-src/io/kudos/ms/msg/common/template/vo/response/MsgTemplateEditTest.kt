package io.kudos.ms.msg.common.template.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgTemplateEdit
 *
 * Covers defaults, full construction, IIdEntity contract and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgTemplateEditTest {

    @Test
    fun defaults() {
        val e = MsgTemplateEdit()
        assertEquals("", e.id)
        assertNull(e.sendTypeDictCode)
        assertNull(e.title)
        assertNull(e.defaultActive)
        assertTrue(e is IIdEntity<*>)
    }

    @Test
    fun fullConstruction() {
        val e = MsgTemplateEdit("t-1", "s", "e", "m", "rg", "en", "t", "c", false, "dt", "dc", "tn")
        assertEquals("t-1", e.id)
        assertEquals("c", e.content)
        assertEquals(false, e.defaultActive)
    }

    @Test
    fun dataClassContract() {
        val a = MsgTemplateEdit(id = "t-1", title = "t")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(title = "x"))
        assertTrue(a.toString().contains("t-1"))
    }

}
