package io.kudos.ms.msg.common.instance.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for MsgInstanceErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix, and the
 * IErrorCodeEnum.displayText contract (prefix + "." + code when the prefix is not blank).
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgInstanceErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        MsgInstanceErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, MsgInstanceErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        MsgInstanceErrorCodeEnum.entries.forEach { entry ->
            assertEquals("msg.error-msg.instance", entry.i18nKeyPrefix)
            assertEquals("msg.error-msg.instance.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Unspecified error", MsgInstanceErrorCodeEnum.UNSPECIFIED.defaultDisplayText)
        assertEquals("Message instance not found", MsgInstanceErrorCodeEnum.INSTANCE_NOT_FOUND.defaultDisplayText)
        assertEquals("Message instance has expired", MsgInstanceErrorCodeEnum.INSTANCE_EXPIRED.defaultDisplayText)
        assertEquals(
            MsgInstanceErrorCodeEnum.INSTANCE_NOT_FOUND,
            MsgInstanceErrorCodeEnum.valueOf("INSTANCE_NOT_FOUND")
        )
    }

}
