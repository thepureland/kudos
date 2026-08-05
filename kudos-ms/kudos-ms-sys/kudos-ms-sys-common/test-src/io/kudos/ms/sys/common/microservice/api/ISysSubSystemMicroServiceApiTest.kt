package io.kudos.ms.sys.common.microservice.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for ISysSubSystemMicroServiceApi
 *
 * Covers all interface methods through a fake implementation that records dispatched
 * arguments, including empty result sets and the boolean returns of unbind / exists.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysSubSystemMicroServiceApiTest {

    private class FakeApi : ISysSubSystemMicroServiceApi {
        var lastBind: Pair<String, Collection<String>>? = null
        var lastUnbind: Pair<String, String>? = null

        override fun getMicroServiceCodesBySubSystemCode(subSystemCode: String): Set<String> =
            if (subSystemCode == "sys") setOf("ms_a", "ms_b") else emptySet()

        override fun getSubSystemCodesByMicroServiceCode(microServiceCode: String): Set<String> =
            if (microServiceCode == "ms_a") setOf("sys") else emptySet()

        override fun batchBind(subSystemCode: String, microServiceCodes: Collection<String>): Int {
            lastBind = subSystemCode to microServiceCodes
            return microServiceCodes.size
        }

        override fun unbind(subSystemCode: String, microServiceCode: String): Boolean {
            lastUnbind = subSystemCode to microServiceCode
            return microServiceCode == "ms_a"
        }

        override fun exists(subSystemCode: String, microServiceCode: String): Boolean =
            subSystemCode == "sys" && microServiceCode == "ms_a"
    }

    @Test
    fun lookups() {
        val api = FakeApi()
        assertEquals(setOf("ms_a", "ms_b"), api.getMicroServiceCodesBySubSystemCode("sys"))
        assertTrue(api.getMicroServiceCodesBySubSystemCode("missing").isEmpty())
        assertEquals(setOf("sys"), api.getSubSystemCodesByMicroServiceCode("ms_a"))
        assertTrue(api.getSubSystemCodesByMicroServiceCode("missing").isEmpty())
    }

    @Test
    fun batchBind() {
        val api = FakeApi()
        assertEquals(2, api.batchBind("sys", listOf("ms_a", "ms_b")))
        assertEquals("sys", api.lastBind?.first)
        assertEquals(listOf("ms_a", "ms_b"), api.lastBind?.second)
        assertEquals(0, api.batchBind("sys", emptyList()))
    }

    @Test
    fun unbind() {
        val api = FakeApi()
        assertTrue(api.unbind("sys", "ms_a"))
        assertEquals("sys" to "ms_a", api.lastUnbind)
        assertFalse(api.unbind("sys", "ms_b"))
    }

    @Test
    fun exists() {
        val api = FakeApi()
        assertTrue(api.exists("sys", "ms_a"))
        assertFalse(api.exists("sys", "ms_b"))
        assertFalse(api.exists("other", "ms_a"))
    }

}
