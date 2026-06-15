package io.kudos.ms.sys.common.microservice.api

import io.kudos.ms.sys.common.microservice.vo.SysMicroServiceCacheEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for ISysMicroServiceApi
 *
 * Covers all interface methods through a fake implementation that records dispatched
 * arguments, including the boolean returns of updateActive.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysMicroServiceApiTest {

    private fun entry(code: String, atomic: Boolean = true, parent: String? = null) = SysMicroServiceCacheEntry(
        id = code, code = code, name = code, context = "/$code",
        atomicService = atomic, parentCode = parent, remark = null, active = true, builtIn = false,
    )

    private inner class FakeApi : ISysMicroServiceApi {
        var lastParentCode: String? = null
        var lastUpdate: Pair<String, Boolean>? = null

        override fun getMicroServiceFromCache(code: String): SysMicroServiceCacheEntry? =
            if (code == "ms_a") entry("ms_a") else null

        override fun getAllMicroServicesFromCache(): List<SysMicroServiceCacheEntry> =
            listOf(entry("ms_a", atomic = true), entry("ms_b", atomic = false))

        override fun getMicroServicesExcludeAtomicFromCache(): List<SysMicroServiceCacheEntry> =
            listOf(entry("ms_b", atomic = false))

        override fun getAtomicServicesFromCache(): List<SysMicroServiceCacheEntry> =
            listOf(entry("ms_a", atomic = true))

        override fun getSubMicroServicesFromCache(parentCode: String): List<SysMicroServiceCacheEntry> {
            lastParentCode = parentCode
            return listOf(entry("child", parent = parentCode))
        }

        override fun getAtomicServicesByParentCodeFromCache(parentCode: String): List<SysMicroServiceCacheEntry> {
            lastParentCode = parentCode
            return listOf(entry("child-atomic", atomic = true, parent = parentCode))
        }

        override fun updateActive(code: String, active: Boolean): Boolean {
            lastUpdate = code to active
            return active
        }
    }

    @Test
    fun getMicroServiceFromCache() {
        val api = FakeApi()
        assertEquals("ms_a", api.getMicroServiceFromCache("ms_a")?.code)
        assertNull(api.getMicroServiceFromCache("missing"))
    }

    @Test
    fun listVariants() {
        val api = FakeApi()
        assertEquals(2, api.getAllMicroServicesFromCache().size)
        assertEquals("ms_b", api.getMicroServicesExcludeAtomicFromCache().single().code)
        assertEquals("ms_a", api.getAtomicServicesFromCache().single().code)
    }

    @Test
    fun byParent() {
        val api = FakeApi()
        assertEquals("parent", api.getSubMicroServicesFromCache("parent").single().parentCode)
        assertEquals("parent", api.lastParentCode)
        assertEquals("parent2", api.getAtomicServicesByParentCodeFromCache("parent2").single().parentCode)
        assertEquals("parent2", api.lastParentCode)
    }

    @Test
    fun updateActive() {
        val api = FakeApi()
        assertTrue(api.updateActive("ms_a", true))
        assertEquals("ms_a" to true, api.lastUpdate)
        assertFalse(api.updateActive("ms_a", false))
        assertEquals("ms_a" to false, api.lastUpdate)
    }

}
