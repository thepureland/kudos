package io.kudos.ms.sys.client.system.fallback

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [SysSystemFallback].
 *
 * Coverage:
 *  - All read methods return their safe defaults (null / empty list), including the
 *    no-arg cache list methods and the code-based queries.
 *  - The write method updateActive returns the failure default false for both flag values.
 *  - Edge inputs: empty strings, Unicode, very long codes.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysSystemFallbackTest {

    private val fallback = SysSystemFallback()

    @Test
    fun getSystemFromCache_returnsNull() {
        assertNull(fallback.getSystemFromCache("sys-1"))
        assertNull(fallback.getSystemFromCache(""))
        assertNull(fallback.getSystemFromCache("系統-ünï"))
        assertNull(fallback.getSystemFromCache("x".repeat(10_000)))
    }

    @Test
    fun getAllSystemsFromCache_returnsEmptyList() {
        assertTrue(fallback.getAllSystemsFromCache().isEmpty())
    }

    @Test
    fun getSystemsExcludeSubSystemFromCache_returnsEmptyList() {
        assertTrue(fallback.getSystemsExcludeSubSystemFromCache().isEmpty())
    }

    @Test
    fun getSubSystemsFromCache_returnsEmptyList() {
        assertTrue(fallback.getSubSystemsFromCache("sys-1").isEmpty())
        assertTrue(fallback.getSubSystemsFromCache("").isEmpty())
    }

    @Test
    fun updateActive_returnsFalse_forBothFlagValues() {
        assertFalse(fallback.updateActive("sys-1", true))
        assertFalse(fallback.updateActive("sys-1", false))
        assertFalse(fallback.updateActive("", true))
    }
}
