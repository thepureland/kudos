package io.kudos.ms.sys.common.tenant.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for ISysTenantResourceApi
 *
 * Covers all interface methods through a fake implementation that records dispatched
 * arguments, including empty result sets and the boolean returns of unbind / exists.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysTenantResourceApiTest {

    private inner class FakeApi : ISysTenantResourceApi {
        var lastBind: Pair<String, Collection<String>>? = null
        var lastUnbind: Pair<String, String>? = null

        override fun getResourceIdsByTenantId(tenantId: String): Set<String> =
            if (tenantId == "t-1") setOf("r-1", "r-2") else emptySet()

        override fun getTenantIdsByResourceId(resourceId: String): Set<String> =
            if (resourceId == "r-1") setOf("t-1") else emptySet()

        override fun batchBind(tenantId: String, resourceIds: Collection<String>): Int {
            lastBind = tenantId to resourceIds
            return resourceIds.size
        }

        override fun unbind(tenantId: String, resourceId: String): Boolean {
            lastUnbind = tenantId to resourceId
            return resourceId == "r-1"
        }

        override fun exists(tenantId: String, resourceId: String): Boolean =
            tenantId == "t-1" && resourceId == "r-1"
    }

    @Test
    fun lookups() {
        val api = FakeApi()
        assertEquals(setOf("r-1", "r-2"), api.getResourceIdsByTenantId("t-1"))
        assertTrue(api.getResourceIdsByTenantId("missing").isEmpty())
        assertEquals(setOf("t-1"), api.getTenantIdsByResourceId("r-1"))
        assertTrue(api.getTenantIdsByResourceId("missing").isEmpty())
    }

    @Test
    fun batchBind() {
        val api = FakeApi()
        assertEquals(2, api.batchBind("t-1", listOf("r-1", "r-2")))
        assertEquals("t-1", api.lastBind?.first)
        assertEquals(0, api.batchBind("t-1", emptyList()))
    }

    @Test
    fun unbind() {
        val api = FakeApi()
        assertTrue(api.unbind("t-1", "r-1"))
        assertEquals("t-1" to "r-1", api.lastUnbind)
        assertFalse(api.unbind("t-1", "r-2"))
    }

    @Test
    fun exists() {
        val api = FakeApi()
        assertTrue(api.exists("t-1", "r-1"))
        assertFalse(api.exists("t-1", "r-2"))
        assertFalse(api.exists("other", "r-1"))
    }

}
