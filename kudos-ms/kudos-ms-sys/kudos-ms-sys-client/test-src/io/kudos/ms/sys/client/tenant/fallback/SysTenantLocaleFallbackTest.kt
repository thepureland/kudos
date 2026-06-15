package io.kudos.ms.sys.client.tenant.fallback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [SysTenantLocaleFallback].
 *
 * Coverage:
 *  - Read methods (getLocaleCodesByTenantId / getTenantIdsByLocaleCode / exists) return
 *    empty set / false safe defaults.
 *  - Write methods (batchBind / unbind) return failure defaults 0 / false.
 *  - Edge inputs: empty strings, Unicode, empty collections.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantLocaleFallbackTest {

    private val fallback = SysTenantLocaleFallback()

    @Test
    fun getLocaleCodesByTenantId_returnsEmptySet() {
        assertTrue(fallback.getLocaleCodesByTenantId("t1").isEmpty())
        assertTrue(fallback.getLocaleCodesByTenantId("").isEmpty())
    }

    @Test
    fun getTenantIdsByLocaleCode_returnsEmptySet() {
        assertTrue(fallback.getTenantIdsByLocaleCode("zh_CN").isEmpty())
        assertTrue(fallback.getTenantIdsByLocaleCode("語言-ünï").isEmpty())
    }

    @Test
    fun batchBind_returnsZero() {
        assertEquals(0, fallback.batchBind("t1", listOf("zh_CN", "en_US")))
        assertEquals(0, fallback.batchBind("t1", emptyList()))
    }

    @Test
    fun unbind_returnsFalse() {
        assertFalse(fallback.unbind("t1", "zh_CN"))
        assertFalse(fallback.unbind("", ""))
    }

    @Test
    fun exists_returnsFalse() {
        assertFalse(fallback.exists("t1", "zh_CN"))
        assertFalse(fallback.exists("", ""))
    }
}
