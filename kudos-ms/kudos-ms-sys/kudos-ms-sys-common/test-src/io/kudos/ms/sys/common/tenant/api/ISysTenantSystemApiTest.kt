package io.kudos.ms.sys.common.tenant.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for ISysTenantSystemApi
 *
 * Covers all interface methods through a fake implementation that records dispatched
 * arguments, including the default null arg of the two grouping methods, empty result
 * sets/maps and the boolean / int returns.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysTenantSystemApiTest {

    private inner class FakeApi : ISysTenantSystemApi {
        var lastGroupTenantIds: Collection<String>? = listOf("SENTINEL")
        var lastGroupTenantIdsCalled = false
        var lastGroupSystemCodes: Collection<String>? = null
        var lastGroupSystemCodesCalled = false
        var lastBind: Pair<String, Collection<String>>? = null
        var lastUnbind: Pair<String, String>? = null
        var lastDeleteTenantId: String? = null
        var lastBatchDeleteTenantIds: Collection<String>? = null

        override fun searchSystemCodesByTenantId(tenantId: String): Set<String> =
            if (tenantId == "t-1") setOf("sys", "crm") else emptySet()

        override fun searchTenantIdsBySystemCode(systemCode: String): Set<String> =
            if (systemCode == "sys") setOf("t-1") else emptySet()

        override fun groupingSystemCodesByTenantIds(tenantIds: Collection<String>?): Map<String, List<String>> {
            lastGroupTenantIdsCalled = true
            lastGroupTenantIds = tenantIds
            return if (tenantIds == null) emptyMap() else tenantIds.associateWith { listOf("sys") }
        }

        override fun groupingTenantIdsBySystemCodes(systemCodes: Collection<String>?): Map<String, List<String>> {
            lastGroupSystemCodesCalled = true
            lastGroupSystemCodes = systemCodes
            return if (systemCodes == null) emptyMap() else systemCodes.associateWith { listOf("t-1") }
        }

        override fun batchBind(tenantId: String, systemCodes: Collection<String>): Int {
            lastBind = tenantId to systemCodes
            return systemCodes.size
        }

        override fun unbind(tenantId: String, systemCode: String): Boolean {
            lastUnbind = tenantId to systemCode
            return systemCode == "sys"
        }

        override fun exists(tenantId: String, systemCode: String): Boolean =
            tenantId == "t-1" && systemCode == "sys"

        override fun deleteByTenantId(tenantId: String): Int {
            lastDeleteTenantId = tenantId
            return if (tenantId == "t-1") 3 else 0
        }

        override fun batchDeleteByTenantIds(tenantIds: Collection<String>): Int {
            lastBatchDeleteTenantIds = tenantIds
            return tenantIds.size
        }
    }

    @Test
    fun lookups() {
        val api = FakeApi()
        assertEquals(setOf("sys", "crm"), api.searchSystemCodesByTenantId("t-1"))
        assertTrue(api.searchSystemCodesByTenantId("missing").isEmpty())
        assertEquals(setOf("t-1"), api.searchTenantIdsBySystemCode("sys"))
        assertTrue(api.searchTenantIdsBySystemCode("missing").isEmpty())
    }

    @Test
    fun groupingSystemCodesByTenantIdsDefaultNull() {
        val api = FakeApi()
        val result = api.groupingSystemCodesByTenantIds()
        assertTrue(api.lastGroupTenantIdsCalled)
        assertNull(api.lastGroupTenantIds)
        assertTrue(result.isEmpty())
    }

    @Test
    fun groupingSystemCodesByTenantIdsExplicit() {
        val api = FakeApi()
        val result = api.groupingSystemCodesByTenantIds(listOf("t-1", "t-2"))
        assertEquals(listOf("t-1", "t-2"), api.lastGroupTenantIds?.toList())
        assertEquals(2, result.size)
        assertEquals(listOf("sys"), result["t-1"])
    }

    @Test
    fun groupingTenantIdsBySystemCodesDefaultNull() {
        val api = FakeApi()
        val result = api.groupingTenantIdsBySystemCodes()
        assertTrue(api.lastGroupSystemCodesCalled)
        assertNull(api.lastGroupSystemCodes)
        assertTrue(result.isEmpty())
    }

    @Test
    fun groupingTenantIdsBySystemCodesExplicit() {
        val api = FakeApi()
        val result = api.groupingTenantIdsBySystemCodes(listOf("sys"))
        assertEquals(listOf("sys"), api.lastGroupSystemCodes?.toList())
        assertEquals(listOf("t-1"), result["sys"])
    }

    @Test
    fun batchBind() {
        val api = FakeApi()
        assertEquals(2, api.batchBind("t-1", listOf("sys", "crm")))
        assertEquals("t-1", api.lastBind?.first)
        assertEquals(0, api.batchBind("t-1", emptyList()))
    }

    @Test
    fun unbindAndExists() {
        val api = FakeApi()
        assertTrue(api.unbind("t-1", "sys"))
        assertEquals("t-1" to "sys", api.lastUnbind)
        assertFalse(api.unbind("t-1", "crm"))
        assertTrue(api.exists("t-1", "sys"))
        assertFalse(api.exists("t-1", "crm"))
        assertFalse(api.exists("other", "sys"))
    }

    @Test
    fun deletes() {
        val api = FakeApi()
        assertEquals(3, api.deleteByTenantId("t-1"))
        assertEquals("t-1", api.lastDeleteTenantId)
        assertEquals(0, api.deleteByTenantId("missing"))
        assertEquals(2, api.batchDeleteByTenantIds(listOf("t-1", "t-2")))
        assertEquals(listOf("t-1", "t-2"), api.lastBatchDeleteTenantIds?.toList())
    }

}
