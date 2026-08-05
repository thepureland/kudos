package io.kudos.ms.msg.common.template.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgTemplateDetail
 *
 * Covers defaults, full construction, IIdEntity contract and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgTemplateDetailTest {

    @Test
    fun defaults() {
        val d = MsgTemplateDetail()
        assertEquals("", d.id)
        assertNull(d.sendTypeDictCode)
        assertNull(d.defaultActive)
        assertNull(d.content)
        assertNull(d.tenantId)
        assertTrue(d is IIdEntity<*>)
    }

    @Test
    fun fullConstruction() {
        val d = MsgTemplateDetail("t-1", "s", "e", "m", "rg", "en", "t", "c", true, "dt", "dc", "tn")
        assertEquals("t-1", d.id)
        assertEquals("rg", d.receiverGroupCode)
        assertEquals(true, d.defaultActive)
        assertEquals("dc", d.defaultContent)
    }

    @Test
    fun dataClassContract() {
        val a = MsgTemplateDetail(id = "t-1", title = "t")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "t-2"))
        assertTrue(a.toString().contains("t-1"))
    }

}
