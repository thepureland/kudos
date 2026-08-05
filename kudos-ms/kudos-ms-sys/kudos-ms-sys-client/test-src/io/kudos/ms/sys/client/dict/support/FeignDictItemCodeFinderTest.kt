package io.kudos.ms.sys.client.dict.support

import io.kudos.context.kit.SpringKit
import io.kudos.ms.sys.client.dict.proxy.ISysDictProxy
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import org.mockito.Mockito
import org.springframework.context.ApplicationContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for [FeignDictItemCodeFinder].
 *
 * Coverage:
 *  - getDictItemCodes maps the proxy's item entries to a set of item codes (deduplicated),
 *    passing dictType / atomicServiceCode through in the right order.
 *  - Empty proxy result yields an empty set.
 *  - The proxy bean is lazily fetched from Spring exactly once and cached for later calls.
 *  - When the Spring context is not initialized, the lazy lookup fails with the
 *    SpringKit "not initialized" error instead of NPE.
 *
 * @author K
 * @since 1.0.0
 */
internal class FeignDictItemCodeFinderTest {

    /** Lightweight fake proxy recording invocations; no Feign involved. */
    private class FakeDictProxy(private val items: List<SysDictItemCacheEntry>) : ISysDictProxy {
        var calls = 0
        var lastDictType: String? = null
        var lastAtomicServiceCode: String? = null

        override fun getActiveDictItems(
            dictType: String,
            atomicServiceCode: String,
        ): List<SysDictItemCacheEntry> {
            calls++
            lastDictType = dictType
            lastAtomicServiceCode = atomicServiceCode
            return items
        }

        override fun getActiveDictItemMap(
            dictType: String,
            atomicServiceCode: String,
        ): LinkedHashMap<String, String> = LinkedHashMap()

        override fun batchGetActiveDictItems(
            dictTypeAndASCodePairs: List<Pair<String, String>>,
        ): Map<Pair<String, String>, List<SysDictItemCacheEntry>> = emptyMap()

        override fun batchGetActiveDictItemMap(
            dictTypeAndASCodePairs: List<Pair<String, String>>,
        ): Map<Pair<String, String>, LinkedHashMap<String, String>> = emptyMap()
    }

    private fun entry(itemCode: String) = SysDictItemCacheEntry(
        id = "id-$itemCode",
        itemCode = itemCode,
        itemName = "name-$itemCode",
        dictId = "dict-1",
        orderNum = null,
        parentId = null,
        remark = null,
        active = true,
        builtIn = false,
        dictType = "sex",
        dictName = "Sex",
        atomicServiceCode = "kudos-sys",
    )

    private fun installContextReturning(proxy: ISysDictProxy): ApplicationContext {
        val ctx = Mockito.mock(ApplicationContext::class.java)
        Mockito.`when`(ctx.getBean(ISysDictProxy::class.java)).thenReturn(proxy)
        SpringKit.applicationContext = ctx
        return ctx
    }

    @AfterTest
    fun tearDown() {
        // Reset global state so other tests are unaffected
        SpringKit.applicationContext = null
    }

    @Test
    fun getDictItemCodes_mapsItemCodesToSet_andPassesParamsInOrder() {
        val proxy = FakeDictProxy(listOf(entry("M"), entry("F"), entry("M"))) // duplicate -> dedup
        installContextReturning(proxy)

        val codes = FeignDictItemCodeFinder().getDictItemCodes("kudos-sys", "sex")

        assertEquals(setOf("M", "F"), codes)
        // IDictItemCodeFinder params are (atomicServiceCode, dictType); the proxy takes (dictType, atomicServiceCode)
        assertEquals("sex", proxy.lastDictType)
        assertEquals("kudos-sys", proxy.lastAtomicServiceCode)
    }

    @Test
    fun getDictItemCodes_emptyProxyResult_yieldsEmptySet() {
        installContextReturning(FakeDictProxy(emptyList()))
        assertTrue(FeignDictItemCodeFinder().getDictItemCodes("kudos-sys", "unknown").isEmpty())
    }

    @Test
    fun getDictItemCodes_fetchesProxyBeanOnlyOnce_thenCachesIt() {
        val proxy = FakeDictProxy(listOf(entry("A")))
        val ctx = installContextReturning(proxy)
        val finder = FeignDictItemCodeFinder()

        finder.getDictItemCodes("kudos-sys", "sex")
        finder.getDictItemCodes("kudos-sys", "sex")
        finder.getDictItemCodes("kudos-sys", "sex")

        Mockito.verify(ctx, Mockito.times(1)).getBean(ISysDictProxy::class.java)
        assertEquals(3, proxy.calls)
    }

    @Test
    fun getDictItemCodes_withoutSpringContext_failsWithNotInitializedError() {
        SpringKit.applicationContext = null
        val ex = assertFailsWith<IllegalStateException> {
            FeignDictItemCodeFinder().getDictItemCodes("kudos-sys", "sex")
        }
        assertTrue(ex.message!!.contains("not initialized"))
    }
}
