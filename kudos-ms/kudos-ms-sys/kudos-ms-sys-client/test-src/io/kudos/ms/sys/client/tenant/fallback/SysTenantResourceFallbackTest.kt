package io.kudos.ms.sys.client.tenant.fallback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [SysTenantResourceFallback].
 *
 * Coverage:
 *  - Read methods (getResourceIdsByTenantId / getTenantIdsByResourceId / exists) return
 *    empty set / false safe defaults — exists=false is the documented "unauthorized" safe default.
 *  - Write methods (batchBind / unbind) return failure defaults 0 / false.
 *  - Edge inputs: empty strings, Unicode, empty collections.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantResourceFallbackTest {

    private val fallback = SysTenantResourceFallback()

    @Test
    fun getResourceIdsByTenantId_returnsEmptySet() {
        assertTrue(fallback.getResourceIdsByTenantId("t1").isEmpty())
        assertTrue(fallback.getResourceIdsByTenantId("").isEmpty())
    }

    @Test
    fun getTenantIdsByResourceId_returnsEmptySet() {
        assertTrue(fallback.getTenantIdsByResourceId("res-1").isEmpty())
        assertTrue(fallback.getTenantIdsByResourceId("資源-ünï").isEmpty())
    }

    @Test
    fun batchBind_returnsZero() {
        assertEquals(0, fallback.batchBind("t1", listOf("r1", "r2")))
        assertEquals(0, fallback.batchBind("t1", emptyList()))
    }

    @Test
    fun unbind_returnsFalse() {
        assertFalse(fallback.unbind("t1", "r1"))
    }

    @Test
    fun exists_returnsFalse_secureDefault() {
        assertFalse(fallback.exists("t1", "r1"))
        assertFalse(fallback.exists("", ""))
    }
}
