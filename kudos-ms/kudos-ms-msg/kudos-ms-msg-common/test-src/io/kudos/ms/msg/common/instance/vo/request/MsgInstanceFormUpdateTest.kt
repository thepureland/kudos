package io.kudos.ms.msg.common.instance.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgInstanceFormUpdate
 *
 * Covers the required id (IIdEntity contract), IMsgInstanceFormBase overrides, null handling
 * and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgInstanceFormUpdateTest {

    @Test
    fun properties() {
        val form = MsgInstanceFormUpdate(
            id = "i-1",
            localeDictCode = "en_US",
            title = "t",
            content = "c",
            templateId = "tpl",
            sendTypeDictCode = "s",
            eventTypeDictCode = "e",
            msgTypeDictCode = "m",
            validTimeStart = LocalDateTime.of(2026, 1, 1, 0, 0),
            validTimeEnd = null,
            tenantId = "tn",
        )
        assertEquals("i-1", form.id)
        assertEquals("en_US", form.localeDictCode)
        assertEquals("t", form.title)
        assertNull(form.validTimeEnd)
        assertTrue(form is IIdEntity<*>)
        assertTrue(form is IMsgInstanceFormBase)
    }

    @Test
    fun dataClassContract() {
        val a = MsgInstanceFormUpdate("i-1", "en_US", "t", "c", "tpl", "s", "e", "m", null, null, "tn")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "i-2"))
        assertTrue(a.toString().contains("i-1"))
    }

}
