package io.kudos.ms.msg.common.template.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgTemplateRow
 *
 * Covers defaults, full construction, IIdEntity contract and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgTemplateRowTest {

    @Test
    fun defaults() {
        val r = MsgTemplateRow()
        assertEquals("", r.id)
        assertNull(r.sendTypeDictCode)
        assertNull(r.defaultContent)
        assertNull(r.tenantId)
        assertTrue(r is IIdEntity<*>)
    }

    @Test
    fun fullConstruction() {
        val r = MsgTemplateRow("t-1", "s", "e", "m", "rg", "en", "t", "c", true, "dt", "dc", "tn")
        assertEquals("t-1", r.id)
        assertEquals("e", r.eventTypeDictCode)
        assertEquals("dt", r.defaultTitle)
    }

    @Test
    fun dataClassContract() {
        val a = MsgTemplateRow(id = "t-1", title = "t")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(tenantId = "tn-2"))
        assertTrue(a.toString().contains("t-1"))
    }

}
