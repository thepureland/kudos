package io.kudos.ms.sys.client.microservice.fallback

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [SysMicroServiceFallback].
 *
 * Coverage:
 *  - All read methods return their safe defaults (null / empty list), including the
 *    no-arg cache list methods and the parentCode-based queries.
 *  - The write method updateActive returns the failure default false.
 *  - Edge inputs: empty strings, Unicode, very long codes, both active flag values.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysMicroServiceFallbackTest {

    private val fallback = SysMicroServiceFallback()

    @Test
    fun getMicroServiceFromCache_returnsNull() {
        assertNull(fallback.getMicroServiceFromCache("ms-sys"))
        assertNull(fallback.getMicroServiceFromCache(""))
        assertNull(fallback.getMicroServiceFromCache("服務-ünï"))
        assertNull(fallback.getMicroServiceFromCache("x".repeat(10_000)))
    }

    @Test
    fun getAllMicroServicesFromCache_returnsEmptyList() {
        assertTrue(fallback.getAllMicroServicesFromCache().isEmpty())
    }

    @Test
    fun getMicroServicesExcludeAtomicFromCache_returnsEmptyList() {
        assertTrue(fallback.getMicroServicesExcludeAtomicFromCache().isEmpty())
    }

    @Test
    fun getAtomicServicesFromCache_returnsEmptyList() {
        assertTrue(fallback.getAtomicServicesFromCache().isEmpty())
    }

    @Test
    fun getSubMicroServicesFromCache_returnsEmptyList() {
        assertTrue(fallback.getSubMicroServicesFromCache("parent").isEmpty())
        assertTrue(fallback.getSubMicroServicesFromCache("").isEmpty())
    }

    @Test
    fun getAtomicServicesByParentCodeFromCache_returnsEmptyList() {
        assertTrue(fallback.getAtomicServicesByParentCodeFromCache("parent").isEmpty())
        assertTrue(fallback.getAtomicServicesByParentCodeFromCache("").isEmpty())
    }

    @Test
    fun updateActive_returnsFalse_forBothFlagValues() {
        assertFalse(fallback.updateActive("ms-sys", true))
        assertFalse(fallback.updateActive("ms-sys", false))
        assertFalse(fallback.updateActive("", true))
    }
}
