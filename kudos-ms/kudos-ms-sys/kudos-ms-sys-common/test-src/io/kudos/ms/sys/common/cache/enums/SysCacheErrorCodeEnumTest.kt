package io.kudos.ms.sys.common.cache.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for SysCacheErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry (codes here are SCxxxxxxxx, not the enum name),
 * i18nKeyPrefix, the IErrorCodeEnum.displayText contract and valueOf round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysCacheErrorCodeEnumTest {

    @Test
    fun entriesHaveDistinctCodeAndDisplayText() {
        SysCacheErrorCodeEnum.entries.forEach { entry ->
            assertTrue(entry.code.startsWith("SC"))
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(3, SysCacheErrorCodeEnum.entries.size)
        // codes are unique
        assertEquals(3, SysCacheErrorCodeEnum.entries.map { it.code }.toSet().size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        SysCacheErrorCodeEnum.entries.forEach { entry ->
            assertEquals("sys.error-msg.cache", entry.i18nKeyPrefix)
            assertEquals("sys.error-msg.cache.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("SC00000001", SysCacheErrorCodeEnum.CACHE_KEY_NOT_FOUND.code)
        assertEquals("Cache key not found", SysCacheErrorCodeEnum.CACHE_KEY_NOT_FOUND.defaultDisplayText)
        assertEquals("SC00000002", SysCacheErrorCodeEnum.CACHE_CONFIG_NOT_FOUND.code)
        assertEquals("SC00000003", SysCacheErrorCodeEnum.CACHE_VALUE_EXPORT_FORBIDDEN.code)
        assertEquals(
            SysCacheErrorCodeEnum.CACHE_VALUE_EXPORT_FORBIDDEN,
            SysCacheErrorCodeEnum.valueOf("CACHE_VALUE_EXPORT_FORBIDDEN")
        )
    }

}
