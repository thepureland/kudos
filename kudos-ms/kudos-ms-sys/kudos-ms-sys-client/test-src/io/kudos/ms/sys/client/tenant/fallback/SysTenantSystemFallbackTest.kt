package io.kudos.ms.sys.client.tenant.fallback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [SysTenantSystemFallback].
 *
 * Coverage:
 *  - Read methods (searchSystemCodesByTenantId / searchTenantIdsBySystemCode /
 *    groupingSystemCodesByTenantIds / groupingTenantIdsBySystemCodes / exists) return
 *    empty set / empty map / false safe defaults, including the nullable-collection
 *    parameters of the grouping methods (null and empty).
 *  - Write methods (batchBind / unbind / deleteByTenantId / batchDeleteByTenantIds)
 *    return failure defaults 0 / false.
 *  - Edge inputs: empty strings, Unicode, empty collections.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantSystemFallbackTest {

    private val fallback = SysTenantSystemFallback()

    @Test
    fun searchSystemCodesByTenantId_returnsEmptySet() {
        assertTrue(fallback.searchSystemCodesByTenantId("t1").isEmpty())
        assertTrue(fallback.searchSystemCodesByTenantId("").isEmpty())
    }

    @Test
    fun searchTenantIdsBySystemCode_returnsEmptySet() {
        assertTrue(fallback.searchTenantIdsBySystemCode("sys-1").isEmpty())
        assertTrue(fallback.searchTenantIdsBySystemCode("系統-ünï").isEmpty())
    }

    @Test
    fun groupingSystemCodesByTenantIds_returnsEmptyMap_forNullAndValues() {
        assertTrue(fallback.groupingSystemCodesByTenantIds(null).isEmpty())
        assertTrue(fallback.groupingSystemCodesByTenantIds(emptyList()).isEmpty())
        assertTrue(fallback.groupingSystemCodesByTenantIds(listOf("t1", "t2")).isEmpty())
    }

    @Test
    fun groupingTenantIdsBySystemCodes_returnsEmptyMap_forNullAndValues() {
        assertTrue(fallback.groupingTenantIdsBySystemCodes(null).isEmpty())
        assertTrue(fallback.groupingTenantIdsBySystemCodes(emptyList()).isEmpty())
        assertTrue(fallback.groupingTenantIdsBySystemCodes(setOf("s1")).isEmpty())
    }

    @Test
    fun batchBind_returnsZero() {
        assertEquals(0, fallback.batchBind("t1", listOf("s1", "s2")))
        assertEquals(0, fallback.batchBind("t1", emptyList()))
    }

    @Test
    fun unbind_returnsFalse() {
        assertFalse(fallback.unbind("t1", "s1"))
    }

    @Test
    fun exists_returnsFalse_secureDefault() {
        assertFalse(fallback.exists("t1", "s1"))
        assertFalse(fallback.exists("", ""))
    }

    @Test
    fun deleteByTenantId_returnsZero() {
        assertEquals(0, fallback.deleteByTenantId("t1"))
        assertEquals(0, fallback.deleteByTenantId(""))
    }

    @Test
    fun batchDeleteByTenantIds_returnsZero() {
        assertEquals(0, fallback.batchDeleteByTenantIds(listOf("t1", "t2")))
        assertEquals(0, fallback.batchDeleteByTenantIds(emptyList()))
    }
}
