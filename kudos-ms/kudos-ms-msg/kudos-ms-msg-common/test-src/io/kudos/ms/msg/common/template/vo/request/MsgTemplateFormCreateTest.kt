package io.kudos.ms.msg.common.template.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgTemplateFormCreate
 *
 * Covers IMsgTemplateFormBase overrides, null handling, the data-class contract, the
 * CONTENT_MAX_LENGTH constant and every @MaxLength constraint declared on the base interface getters.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgTemplateFormCreateTest {

    @Test
    fun properties() {
        val form: IMsgTemplateFormBase = MsgTemplateFormCreate(
            sendTypeDictCode = "s",
            eventTypeDictCode = "e",
            msgTypeDictCode = "m",
            receiverGroupCode = "rg",
            localeDictCode = "en",
            title = "t",
            content = "c",
            defaultActive = true,
            defaultTitle = "dt",
            defaultContent = "dc",
            tenantId = "tn",
        )
        assertEquals("s", form.sendTypeDictCode)
        assertEquals("e", form.eventTypeDictCode)
        assertEquals("m", form.msgTypeDictCode)
        assertEquals("rg", form.receiverGroupCode)
        assertEquals("en", form.localeDictCode)
        assertEquals("t", form.title)
        assertEquals("c", form.content)
        assertEquals(true, form.defaultActive)
        assertEquals("dt", form.defaultTitle)
        assertEquals("dc", form.defaultContent)
        assertEquals("tn", form.tenantId)
    }

    @Test
    fun allNull() {
        val form = MsgTemplateFormCreate(null, null, null, null, null, null, null, null, null, null, null)
        assertNull(form.sendTypeDictCode)
        assertNull(form.defaultActive)
        assertNull(form.content)
        assertNull(form.tenantId)
    }

    @Test
    fun dataClassContract() {
        val a = MsgTemplateFormCreate("s", "e", "m", "rg", "en", "t", "c", true, "dt", "dc", "tn")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(title = "x"))
        assertTrue(a.toString().contains("rg"))
    }

    @Test
    fun contentMaxLengthConstant() {
        assertEquals(65535, IMsgTemplateFormBase.CONTENT_MAX_LENGTH)
    }

    @Test
    fun maxLengthConstraintsOnBaseGetters() {
        val expected = mapOf(
            "sendTypeDictCode" to 6,
            "eventTypeDictCode" to 32,
            "msgTypeDictCode" to 16,
            "receiverGroupCode" to 36,
            "localeDictCode" to 5,
            "title" to 256,
            "content" to IMsgTemplateFormBase.CONTENT_MAX_LENGTH,
            "defaultTitle" to 256,
            "defaultContent" to IMsgTemplateFormBase.CONTENT_MAX_LENGTH,
            "tenantId" to 36,
        )
        val props = IMsgTemplateFormBase::class.memberProperties.associateBy { it.name }
        expected.forEach { (name, max) ->
            val ann = props.getValue(name).getter.findAnnotation<MaxLength>()
            assertNotNull(ann, "$name getter should be annotated with @MaxLength")
            assertEquals(max, ann.max, "@MaxLength.max mismatch on $name")
        }
        // defaultActive carries no MaxLength (it is a Boolean, not a CharSequence)
        assertNull(props.getValue("defaultActive").getter.findAnnotation<MaxLength>())
    }

}
