package io.kudos.ms.sys.common.system.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for SysSystemErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix, the
 * IErrorCodeEnum.displayText contract and valueOf round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysSystemErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        SysSystemErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, SysSystemErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        SysSystemErrorCodeEnum.entries.forEach { entry ->
            assertEquals("sys.error-msg.system", entry.i18nKeyPrefix)
            assertEquals("sys.error-msg.system.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Unspecified error", SysSystemErrorCodeEnum.UNSPECIFIED.defaultDisplayText)
        assertEquals("System not found", SysSystemErrorCodeEnum.SYSTEM_NOT_FOUND.defaultDisplayText)
        assertEquals(
            "System code already exists",
            SysSystemErrorCodeEnum.SYSTEM_CODE_ALREADY_EXISTS.defaultDisplayText
        )
        assertEquals(
            SysSystemErrorCodeEnum.SYSTEM_CODE_ALREADY_EXISTS,
            SysSystemErrorCodeEnum.valueOf("SYSTEM_CODE_ALREADY_EXISTS")
        )
    }

}
