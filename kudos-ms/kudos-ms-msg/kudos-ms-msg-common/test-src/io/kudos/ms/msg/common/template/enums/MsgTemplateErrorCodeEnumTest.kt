package io.kudos.ms.msg.common.template.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for MsgTemplateErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix and the displayText contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgTemplateErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        MsgTemplateErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, MsgTemplateErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        MsgTemplateErrorCodeEnum.entries.forEach { entry ->
            assertEquals("msg.error-msg.template", entry.i18nKeyPrefix)
            assertEquals("msg.error-msg.template.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Unspecified error", MsgTemplateErrorCodeEnum.UNSPECIFIED.defaultDisplayText)
        assertEquals("Message template not found", MsgTemplateErrorCodeEnum.TEMPLATE_NOT_FOUND.defaultDisplayText)
        assertEquals(
            "A template with the same event / message type / locale already exists for this tenant",
            MsgTemplateErrorCodeEnum.DUPLICATE_TEMPLATE.defaultDisplayText
        )
        assertEquals(MsgTemplateErrorCodeEnum.DUPLICATE_TEMPLATE, MsgTemplateErrorCodeEnum.valueOf("DUPLICATE_TEMPLATE"))
    }

}
