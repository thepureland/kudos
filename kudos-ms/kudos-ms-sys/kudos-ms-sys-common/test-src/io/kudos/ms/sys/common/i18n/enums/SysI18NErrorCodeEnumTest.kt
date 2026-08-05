package io.kudos.ms.sys.common.i18n.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for SysI18NErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix, the
 * IErrorCodeEnum.displayText contract (prefix + "." + code when the prefix is not blank),
 * and valueOf round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysI18NErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        SysI18NErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, SysI18NErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        SysI18NErrorCodeEnum.entries.forEach { entry ->
            assertEquals("sys.error-msg.i18n", entry.i18nKeyPrefix)
            assertEquals("sys.error-msg.i18n.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("UNSPECIFIED", SysI18NErrorCodeEnum.UNSPECIFIED.code)
        assertEquals("Unspecified error", SysI18NErrorCodeEnum.UNSPECIFIED.defaultDisplayText)
        assertEquals("I18N_NOT_FOUND", SysI18NErrorCodeEnum.I18N_NOT_FOUND.code)
        assertEquals("I18n entry does not exist", SysI18NErrorCodeEnum.I18N_NOT_FOUND.defaultDisplayText)
        assertEquals(
            SysI18NErrorCodeEnum.I18N_ALREADY_EXISTS,
            SysI18NErrorCodeEnum.valueOf("I18N_ALREADY_EXISTS")
        )
    }

}
