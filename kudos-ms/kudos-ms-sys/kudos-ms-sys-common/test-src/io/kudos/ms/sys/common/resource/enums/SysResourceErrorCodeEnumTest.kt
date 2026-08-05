package io.kudos.ms.sys.common.resource.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for SysResourceErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix, the
 * IErrorCodeEnum.displayText contract and valueOf round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysResourceErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        SysResourceErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(4, SysResourceErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        SysResourceErrorCodeEnum.entries.forEach { entry ->
            assertEquals("sys.error-msg.resource", entry.i18nKeyPrefix)
            assertEquals("sys.error-msg.resource.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Resource not found", SysResourceErrorCodeEnum.RESOURCE_NOT_FOUND.defaultDisplayText)
        assertEquals(
            "A resource with the same URL already exists under this sub-system",
            SysResourceErrorCodeEnum.RESOURCE_URL_ALREADY_EXISTS.defaultDisplayText
        )
        assertEquals(
            "Parent resource not found or disabled",
            SysResourceErrorCodeEnum.PARENT_RESOURCE_NOT_FOUND.defaultDisplayText
        )
        assertEquals(
            SysResourceErrorCodeEnum.RESOURCE_NOT_FOUND,
            SysResourceErrorCodeEnum.valueOf("RESOURCE_NOT_FOUND")
        )
    }

}
