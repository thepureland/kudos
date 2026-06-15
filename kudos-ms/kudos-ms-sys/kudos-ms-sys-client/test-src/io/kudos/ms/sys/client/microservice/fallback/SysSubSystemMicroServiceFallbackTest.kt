package io.kudos.ms.sys.client.microservice.fallback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [SysSubSystemMicroServiceFallback].
 *
 * Coverage:
 *  - Read methods (getMicroServiceCodesBySubSystemCode / getSubSystemCodesByMicroServiceCode /
 *    exists) return empty set / false safe defaults.
 *  - Write methods (batchBind / unbind) return failure defaults 0 / false.
 *  - Edge inputs: empty strings, Unicode, empty collections.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysSubSystemMicroServiceFallbackTest {

    private val fallback = SysSubSystemMicroServiceFallback()

    @Test
    fun getMicroServiceCodesBySubSystemCode_returnsEmptySet() {
        assertTrue(fallback.getMicroServiceCodesBySubSystemCode("sub-1").isEmpty())
        assertTrue(fallback.getMicroServiceCodesBySubSystemCode("").isEmpty())
    }

    @Test
    fun getSubSystemCodesByMicroServiceCode_returnsEmptySet() {
        assertTrue(fallback.getSubSystemCodesByMicroServiceCode("ms-1").isEmpty())
        assertTrue(fallback.getSubSystemCodesByMicroServiceCode("服務-ünï").isEmpty())
    }

    @Test
    fun batchBind_returnsZero() {
        assertEquals(0, fallback.batchBind("sub-1", listOf("ms-1", "ms-2")))
        assertEquals(0, fallback.batchBind("sub-1", emptyList()))
    }

    @Test
    fun unbind_returnsFalse() {
        assertFalse(fallback.unbind("sub-1", "ms-1"))
        assertFalse(fallback.unbind("", ""))
    }

    @Test
    fun exists_returnsFalse() {
        assertFalse(fallback.exists("sub-1", "ms-1"))
        assertFalse(fallback.exists("", ""))
    }
}
