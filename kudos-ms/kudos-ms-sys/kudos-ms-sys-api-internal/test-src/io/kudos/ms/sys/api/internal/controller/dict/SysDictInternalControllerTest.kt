package io.kudos.ms.sys.api.internal.controller.dict

import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.api.SysDictApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for SysDictInternalController (pure delegation + HTTP batch adapters, core api mocked with Mockito,
 * no Spring container)
 *
 * Coverage:
 * - getActiveDictItems / getActiveDictItemMap delegate (incl. empty results)
 * - batchGetActiveDictItems / batchGetActiveDictItemMap delegate unchanged (incl. empty input)
 * - batchGetActiveDictItemsHttp / batchGetActiveDictItemMapHttp: List<List<String>> -> Pair conversion,
 *   "first|second" key concatenation (flipped key order is produced by the core api, the adapter only joins),
 *   extra elements beyond index 1 ignored, empty input, and IndexOutOfBoundsException on malformed
 *   single-element pairs
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictInternalControllerTest {

    private val api = mock(SysDictApi::class.java)
    private val controller = SysDictInternalController(api)

    @Test
    fun getActiveDictItems_delegates() {
        val items = listOf(mock(SysDictItemCacheEntry::class.java))
        whenCalled(api.getActiveDictItems("dt-a", "as-a")).thenReturn(items)

        assertSame(items, controller.getActiveDictItems("dt-a", "as-a"))
        assertTrue(controller.getActiveDictItems("dt-none", "as-a").isEmpty())
        verify(api).getActiveDictItems("dt-a", "as-a")
    }

    @Test
    fun getActiveDictItemMap_delegates() {
        val map = linkedMapOf("code-1" to "name-1", "code-2" to "name-2")
        whenCalled(api.getActiveDictItemMap("dt-a", "as-a")).thenReturn(map)
        whenCalled(api.getActiveDictItemMap("dt-none", "as-a")).thenReturn(LinkedHashMap())

        assertSame(map, controller.getActiveDictItemMap("dt-a", "as-a"))
        assertTrue(controller.getActiveDictItemMap("dt-none", "as-a").isEmpty())
    }

    @Test
    fun batchGetActiveDictItems_delegates() {
        val pairs = listOf("dt-a" to "as-a")
        val result = mapOf(("as-a" to "dt-a") to listOf(mock(SysDictItemCacheEntry::class.java)))
        whenCalled(api.batchGetActiveDictItems(pairs)).thenReturn(result)

        assertSame(result, controller.batchGetActiveDictItems(pairs))
        assertTrue(controller.batchGetActiveDictItems(emptyList()).isEmpty())
        verify(api).batchGetActiveDictItems(pairs)
        verify(api).batchGetActiveDictItems(emptyList())
    }

    @Test
    fun batchGetActiveDictItemMap_delegates() {
        val pairs = listOf("dt-a" to "as-a")
        val result = mapOf(("as-a" to "dt-a") to linkedMapOf("code-1" to "name-1"))
        whenCalled(api.batchGetActiveDictItemMap(pairs)).thenReturn(result)

        assertSame(result, controller.batchGetActiveDictItemMap(pairs))
        assertTrue(controller.batchGetActiveDictItemMap(emptyList()).isEmpty())
    }

    @Test
    fun batchGetActiveDictItemsHttp_convertsPairsAndConcatenatesKeys() {
        val items = listOf(mock(SysDictItemCacheEntry::class.java))
        // the core api keys its result by the flipped pair (atomicServiceCode, dictType)
        whenCalled(api.batchGetActiveDictItems(listOf("dt-a" to "as-a")))
            .thenReturn(mapOf(("as-a" to "dt-a") to items))

        val result = controller.batchGetActiveDictItemsHttp(listOf(listOf("dt-a", "as-a")))
        assertEquals(setOf("as-a|dt-a"), result.keys)
        assertSame(items, result["as-a|dt-a"])
    }

    @Test
    fun batchGetActiveDictItemsHttp_ignoresExtraElementsAndHandlesEmptyInput() {
        val items = listOf(mock(SysDictItemCacheEntry::class.java))
        whenCalled(api.batchGetActiveDictItems(listOf("dt-a" to "as-a")))
            .thenReturn(mapOf(("as-a" to "dt-a") to items))

        // elements beyond index 1 are ignored by the adapter
        val result = controller.batchGetActiveDictItemsHttp(listOf(listOf("dt-a", "as-a", "ignored")))
        assertEquals(setOf("as-a|dt-a"), result.keys)

        assertTrue(controller.batchGetActiveDictItemsHttp(emptyList()).isEmpty())
    }

    @Test
    fun batchGetActiveDictItemsHttp_failsOnMalformedPair() {
        assertFailsWith<IndexOutOfBoundsException> {
            controller.batchGetActiveDictItemsHttp(listOf(listOf("only-one-element")))
        }
    }

    @Test
    fun batchGetActiveDictItemMapHttp_convertsPairsAndConcatenatesKeys() {
        val map = linkedMapOf("code-1" to "name-1")
        whenCalled(api.batchGetActiveDictItemMap(listOf("dt-a" to "as-a")))
            .thenReturn(mapOf(("as-a" to "dt-a") to map))

        val result = controller.batchGetActiveDictItemMapHttp(listOf(listOf("dt-a", "as-a")))
        assertEquals(setOf("as-a|dt-a"), result.keys)
        assertSame(map, result["as-a|dt-a"])
    }

    @Test
    fun batchGetActiveDictItemMapHttp_handlesEmptyInputAndMalformedPair() {
        assertTrue(controller.batchGetActiveDictItemMapHttp(emptyList()).isEmpty())
        assertFailsWith<IndexOutOfBoundsException> {
            controller.batchGetActiveDictItemMapHttp(listOf(emptyList()))
        }
    }
}
