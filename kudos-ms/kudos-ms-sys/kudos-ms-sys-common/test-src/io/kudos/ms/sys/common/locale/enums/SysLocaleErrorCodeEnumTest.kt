package io.kudos.ms.sys.common.locale.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for SysLocaleErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix, the
 * IErrorCodeEnum.displayText contract and valueOf round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysLocaleErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        SysLocaleErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, SysLocaleErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        SysLocaleErrorCodeEnum.entries.forEach { entry ->
            assertEquals("sys.error-msg.locale", entry.i18nKeyPrefix)
            assertEquals("sys.error-msg.locale.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Language code does not exist", SysLocaleErrorCodeEnum.LOCALE_NOT_FOUND.defaultDisplayText)
        assertEquals(
            SysLocaleErrorCodeEnum.LOCALE_ALREADY_EXISTS,
            SysLocaleErrorCodeEnum.valueOf("LOCALE_ALREADY_EXISTS")
        )
    }

}
