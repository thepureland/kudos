package io.kudos.ms.sys.common.tenant.api

import io.kudos.ms.sys.common.tenant.vo.SysTenantCacheEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for ISysTenantApi
 *
 * Covers all interface methods through a fake implementation that records dispatched
 * arguments, including the not-found null return, empty collections and map results.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysTenantApiTest {

    private fun entry(id: String) = SysTenantCacheEntry(
        id = id, name = "name-$id", timezone = null, defaultLanguageCode = null,
        remark = null, active = true, builtIn = false,
    )

    private inner class FakeApi : ISysTenantApi {
        var lastIds: Collection<String>? = null

        override fun getTenantFromCache(id: String): SysTenantCacheEntry? =
            if (id == "missing") null else entry(id)

        override fun getTenantsFromCacheByIds(ids: Collection<String>): Map<String, SysTenantCacheEntry> {
            lastIds = ids
            return ids.associateWith { entry(it) }
        }

        override fun getTenantsBySubSystemCode(subSystemCode: String): List<SysTenantCacheEntry> =
            if (subSystemCode == "sys") listOf(entry("t-1")) else emptyList()
    }

    @Test
    fun getTenantFromCache() {
        val api = FakeApi()
        assertEquals("t-1", api.getTenantFromCache("t-1")?.id)
        assertNull(api.getTenantFromCache("missing"))
    }

    @Test
    fun getTenantsFromCacheByIds() {
        val api = FakeApi()
        val result = api.getTenantsFromCacheByIds(setOf("t-1", "t-2"))
        assertEquals(setOf("t-1", "t-2"), api.lastIds?.toSet())
        assertEquals(2, result.size)
        assertEquals("t-1", result["t-1"]?.id)
        assertTrue(api.getTenantsFromCacheByIds(emptyList()).isEmpty())
    }

    @Test
    fun getTenantsBySubSystemCode() {
        val api = FakeApi()
        assertEquals(1, api.getTenantsBySubSystemCode("sys").size)
        assertTrue(api.getTenantsBySubSystemCode("missing").isEmpty())
    }

}
