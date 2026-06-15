package io.kudos.ms.msg.common.instance.vo.request

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgInstanceFormCreate
 *
 * Covers property accessors (including IMsgInstanceFormBase overrides), null handling,
 * and the data-class equals/hashCode/copy/toString contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgInstanceFormCreateTest {

    @Test
    fun properties() {
        val start = LocalDateTime.of(2026, 1, 1, 0, 0)
        val end = LocalDateTime.of(2026, 2, 1, 0, 0)
        val form: IMsgInstanceFormBase = MsgInstanceFormCreate(
            localeDictCode = "en_US",
            title = "t",
            content = "c",
            templateId = "tpl",
            sendTypeDictCode = "s",
            eventTypeDictCode = "e",
            msgTypeDictCode = "m",
            validTimeStart = start,
            validTimeEnd = end,
            tenantId = "tn",
        )
        assertEquals("en_US", form.localeDictCode)
        assertEquals("t", form.title)
        assertEquals("c", form.content)
        assertEquals("tpl", form.templateId)
        assertEquals("s", form.sendTypeDictCode)
        assertEquals("e", form.eventTypeDictCode)
        assertEquals("m", form.msgTypeDictCode)
        assertEquals(start, form.validTimeStart)
        assertEquals(end, form.validTimeEnd)
        assertEquals("tn", form.tenantId)
    }

    @Test
    fun allNull() {
        val form = MsgInstanceFormCreate(null, null, null, null, null, null, null, null, null, null)
        assertNull(form.localeDictCode)
        assertNull(form.title)
        assertNull(form.content)
        assertNull(form.templateId)
        assertNull(form.validTimeStart)
        assertNull(form.tenantId)
    }

    @Test
    fun dataClassContract() {
        val a = MsgInstanceFormCreate("en_US", "t", "c", "tpl", "s", "e", "m", null, null, "tn")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(title = "x"))
        assertTrue(a.toString().contains("en_US"))
    }

}
