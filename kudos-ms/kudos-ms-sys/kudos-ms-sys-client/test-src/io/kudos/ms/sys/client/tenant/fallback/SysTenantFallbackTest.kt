package io.kudos.ms.sys.client.tenant.fallback

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [SysTenantFallback].
 *
 * Coverage:
 *  - Every overridden read method returns its documented safe default (null / empty map /
 *    empty list) and never throws — including empty strings, Unicode ids, very long ids,
 *    empty and multi-element id collections.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantFallbackTest {

    private val fallback = SysTenantFallback()

    @Test
    fun getTenantFromCache_returnsNull() {
        assertNull(fallback.getTenantFromCache("tenant-1"))
        assertNull(fallback.getTenantFromCache(""))
        assertNull(fallback.getTenantFromCache("租戶-ünïcødé"))
        assertNull(fallback.getTenantFromCache("x".repeat(10_000)))
    }

    @Test
    fun getTenantsFromCacheByIds_returnsEmptyMap() {
        assertTrue(fallback.getTenantsFromCacheByIds(emptyList()).isEmpty())
        assertTrue(fallback.getTenantsFromCacheByIds(listOf("a", "b", "")).isEmpty())
        assertTrue(fallback.getTenantsFromCacheByIds(setOf("租戶")).isEmpty())
    }

    @Test
    fun getTenantsBySubSystemCode_returnsEmptyList() {
        assertTrue(fallback.getTenantsBySubSystemCode("sub-sys").isEmpty())
        assertTrue(fallback.getTenantsBySubSystemCode("").isEmpty())
    }
}
