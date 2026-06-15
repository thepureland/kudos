package io.kudos.ms.sys.common.outline.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for SysOutLineErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix, the
 * IErrorCodeEnum.displayText contract and valueOf round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysOutLineErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        SysOutLineErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, SysOutLineErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        SysOutLineErrorCodeEnum.entries.forEach { entry ->
            assertEquals("sys.error-msg.outline", entry.i18nKeyPrefix)
            assertEquals("sys.error-msg.outline.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Outbound whitelist entry does not exist", SysOutLineErrorCodeEnum.OUT_LINE_NOT_FOUND.defaultDisplayText)
        assertEquals(
            SysOutLineErrorCodeEnum.OUT_LINE_ALREADY_EXISTS,
            SysOutLineErrorCodeEnum.valueOf("OUT_LINE_ALREADY_EXISTS")
        )
    }

}
