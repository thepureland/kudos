package io.kudos.ms.sys.common.tenant.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for ISysTenantLocaleApi
 *
 * Covers all interface methods through a fake implementation that records dispatched
 * arguments, including empty result sets and the boolean returns of unbind / exists.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysTenantLocaleApiTest {

    private inner class FakeApi : ISysTenantLocaleApi {
        var lastBind: Pair<String, Collection<String>>? = null
        var lastUnbind: Pair<String, String>? = null

        override fun getLocaleCodesByTenantId(tenantId: String): Set<String> =
            if (tenantId == "t-1") setOf("zh-CN", "en-US") else emptySet()

        override fun getTenantIdsByLocaleCode(localeCode: String): Set<String> =
            if (localeCode == "zh-CN") setOf("t-1") else emptySet()

        override fun batchBind(tenantId: String, localeCodes: Collection<String>): Int {
            lastBind = tenantId to localeCodes
            return localeCodes.size
        }

        override fun unbind(tenantId: String, localeCode: String): Boolean {
            lastUnbind = tenantId to localeCode
            return localeCode == "zh-CN"
        }

        override fun exists(tenantId: String, localeCode: String): Boolean =
            tenantId == "t-1" && localeCode == "zh-CN"
    }

    @Test
    fun lookups() {
        val api = FakeApi()
        assertEquals(setOf("zh-CN", "en-US"), api.getLocaleCodesByTenantId("t-1"))
        assertTrue(api.getLocaleCodesByTenantId("missing").isEmpty())
        assertEquals(setOf("t-1"), api.getTenantIdsByLocaleCode("zh-CN"))
        assertTrue(api.getTenantIdsByLocaleCode("missing").isEmpty())
    }

    @Test
    fun batchBind() {
        val api = FakeApi()
        assertEquals(2, api.batchBind("t-1", listOf("zh-CN", "en-US")))
        assertEquals("t-1", api.lastBind?.first)
        assertEquals(0, api.batchBind("t-1", emptyList()))
    }

    @Test
    fun unbind() {
        val api = FakeApi()
        assertTrue(api.unbind("t-1", "zh-CN"))
        assertEquals("t-1" to "zh-CN", api.lastUnbind)
        assertFalse(api.unbind("t-1", "en-US"))
    }

    @Test
    fun exists() {
        val api = FakeApi()
        assertTrue(api.exists("t-1", "zh-CN"))
        assertFalse(api.exists("t-1", "en-US"))
        assertFalse(api.exists("other", "zh-CN"))
    }

}
