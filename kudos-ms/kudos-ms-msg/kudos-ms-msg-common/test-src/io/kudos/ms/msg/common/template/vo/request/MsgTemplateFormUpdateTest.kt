package io.kudos.ms.msg.common.template.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for MsgTemplateFormUpdate
 *
 * Covers the required id (IIdEntity), IMsgTemplateFormBase overrides and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgTemplateFormUpdateTest {

    @Test
    fun properties() {
        val form = MsgTemplateFormUpdate(
            id = "t-1",
            sendTypeDictCode = "s",
            eventTypeDictCode = "e",
            msgTypeDictCode = "m",
            receiverGroupCode = "rg",
            localeDictCode = "en",
            title = "t",
            content = "c",
            defaultActive = false,
            defaultTitle = "dt",
            defaultContent = "dc",
            tenantId = "tn",
        )
        assertEquals("t-1", form.id)
        assertEquals("s", form.sendTypeDictCode)
        assertEquals(false, form.defaultActive)
        assertTrue(form is IIdEntity<*>)
        assertTrue(form is IMsgTemplateFormBase)
    }

    @Test
    fun dataClassContract() {
        val a = MsgTemplateFormUpdate("t-1", "s", "e", "m", "rg", "en", "t", "c", true, "dt", "dc", "tn")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "t-2"))
        assertTrue(a.toString().contains("t-1"))
    }

}
