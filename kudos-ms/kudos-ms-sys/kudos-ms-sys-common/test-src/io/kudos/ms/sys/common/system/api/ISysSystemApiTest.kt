package io.kudos.ms.sys.common.system.api

import io.kudos.ms.sys.common.system.vo.SysSystemCacheEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for ISysSystemApi
 *
 * Covers all interface methods through a fake implementation that records dispatched
 * arguments, including the not-found null return, empty list results and the boolean
 * return of updateActive.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysSystemApiTest {

    private fun entry(code: String) = SysSystemCacheEntry(
        id = code, code = code, name = "name-$code", subSystem = false,
        parentCode = null, remark = null, active = true, builtIn = false,
    )

    private inner class FakeApi : ISysSystemApi {
        var lastUpdate: Pair<String, Boolean>? = null

        override fun getSystemFromCache(code: String): SysSystemCacheEntry? =
            if (code == "missing") null else entry(code)

        override fun getAllSystemsFromCache(): List<SysSystemCacheEntry> =
            listOf(entry("sys"), entry("crm"))

        override fun getSystemsExcludeSubSystemFromCache(): List<SysSystemCacheEntry> =
            listOf(entry("sys"))

        override fun updateActive(code: String, active: Boolean): Boolean {
            lastUpdate = code to active
            return code != "missing"
        }

        override fun getSubSystemsFromCache(systemCode: String): List<SysSystemCacheEntry> =
            if (systemCode == "sys") listOf(entry("sub")) else emptyList()
    }

    @Test
    fun getSystemFromCache() {
        val api = FakeApi()
        assertEquals("sys", api.getSystemFromCache("sys")?.code)
        assertNull(api.getSystemFromCache("missing"))
    }

    @Test
    fun listMethods() {
        val api = FakeApi()
        assertEquals(2, api.getAllSystemsFromCache().size)
        assertEquals(1, api.getSystemsExcludeSubSystemFromCache().size)
    }

    @Test
    fun updateActive() {
        val api = FakeApi()
        assertTrue(api.updateActive("sys", false))
        assertEquals("sys" to false, api.lastUpdate)
        assertFalse(api.updateActive("missing", true))
    }

    @Test
    fun getSubSystemsFromCache() {
        val api = FakeApi()
        assertEquals(1, api.getSubSystemsFromCache("sys").size)
        assertTrue(api.getSubSystemsFromCache("missing").isEmpty())
    }

}
