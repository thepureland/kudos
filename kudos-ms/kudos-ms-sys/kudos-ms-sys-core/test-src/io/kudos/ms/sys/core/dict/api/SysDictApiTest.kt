package io.kudos.ms.sys.core.dict.api

import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.service.iservice.ISysDictService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for SysDictApi (pure delegation, service mocked with Mockito, no Spring container).
 *
 * Coverage:
 * - getActiveDictItems / getActiveDictItemMap delegate to the service unchanged (hit and empty)
 * - batchGetActiveDictItems / batchGetActiveDictItemMap delegate unchanged (incl. empty input)
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictApiTest {

    private val service = mock(ISysDictService::class.java)
    private val api = SysDictApi(service)

    private fun item(id: String) = SysDictItemCacheEntry(
        id = id,
        itemCode = "code-$id",
        itemName = "name-$id",
        dictId = "dict-1",
        orderNum = 1,
        parentId = null,
        remark = null,
        active = true,
        builtIn = false,
        dictType = "type-1",
        dictName = "dict-name-1",
        atomicServiceCode = "svc-1",
    )

    @Test
    fun getActiveDictItems_delegates() {
        val list = listOf(item("i1"))
        whenCalled(service.getActiveDictItemsFromCache("type-1", "svc-1")).thenReturn(list)
        assertSame(list, api.getActiveDictItems("type-1", "svc-1"))
        verify(service).getActiveDictItemsFromCache("type-1", "svc-1")
    }

    @Test
    fun getActiveDictItems_emptyWhenServiceReturnsNothing() {
        whenCalled(service.getActiveDictItemsFromCache("t", "s")).thenReturn(emptyList())
        assertTrue(api.getActiveDictItems("t", "s").isEmpty())
    }

    @Test
    fun getActiveDictItemMap_delegates() {
        val map = linkedMapOf("c1" to "n1", "c2" to "n2")
        whenCalled(service.getActiveDictItemMapFromCache("type-1", "svc-1")).thenReturn(map)
        assertEquals(map, api.getActiveDictItemMap("type-1", "svc-1"))
        verify(service).getActiveDictItemMapFromCache("type-1", "svc-1")
    }

    @Test
    fun batchGetActiveDictItems_delegates() {
        val key = "type-1" to "svc-1"
        val result = mapOf(key to listOf(item("i1")))
        val pairs = listOf(key)
        whenCalled(service.batchGetActiveDictItemsFromCache(pairs)).thenReturn(result)
        assertEquals(result, api.batchGetActiveDictItems(pairs))

        whenCalled(service.batchGetActiveDictItemsFromCache(emptyList())).thenReturn(emptyMap())
        assertTrue(api.batchGetActiveDictItems(emptyList()).isEmpty())
    }

    @Test
    fun batchGetActiveDictItemMap_delegates() {
        val key = "type-1" to "svc-1"
        val result = mapOf(key to linkedMapOf("c1" to "n1"))
        val pairs = listOf(key)
        whenCalled(service.batchGetActiveDictItemMapFromCache(pairs)).thenReturn(result)
        assertEquals(result, api.batchGetActiveDictItemMap(pairs))

        whenCalled(service.batchGetActiveDictItemMapFromCache(emptyList())).thenReturn(emptyMap())
        assertTrue(api.batchGetActiveDictItemMap(emptyList()).isEmpty())
    }
}
