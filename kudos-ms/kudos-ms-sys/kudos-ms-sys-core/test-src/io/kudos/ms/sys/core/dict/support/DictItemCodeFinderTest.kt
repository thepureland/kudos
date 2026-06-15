package io.kudos.ms.sys.core.dict.support

import io.kudos.context.kit.SpringKit
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.cache.SysDictItemHashCache
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenCalled
import org.springframework.context.ApplicationContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure unit test for [DictItemCodeFinder].
 *
 * The finder lazily resolves [SysDictItemHashCache] via [SpringKit] (it is loaded by Java ServiceLoader so cannot
 * use bean injection). Here we install a mocked [ApplicationContext] into [SpringKit] and verify:
 * - item codes are mapped from the cache entries and de-duplicated into a Set
 * - empty cache yields an empty Set
 * - the resolved bean is cached after the first call (the context is queried only once)
 *
 * @author K
 * @since 1.0.0
 */
internal class DictItemCodeFinderTest {

    private val savedContext: ApplicationContext? = runCatching { SpringKit.applicationContext }.getOrNull()

    @AfterTest
    fun restore() {
        SpringKit.applicationContext = savedContext
    }

    private fun entry(itemCode: String) = SysDictItemCacheEntry(
        id = "id-$itemCode",
        itemCode = itemCode,
        itemName = "name-$itemCode",
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

    private fun installCache(cache: SysDictItemHashCache): ApplicationContext {
        val ctx = mock(ApplicationContext::class.java)
        whenCalled(ctx.getBean(SysDictItemHashCache::class.java)).thenReturn(cache)
        SpringKit.applicationContext = ctx
        return ctx
    }

    @Test
    fun getDictItemCodes_mapsAndDeduplicates() {
        val cache = mock(SysDictItemHashCache::class.java)
        // two entries share the same itemCode "dup" -> Set must collapse to one
        whenCalled(cache.getDictItems("svc-1", "type-1"))
            .thenReturn(listOf(entry("a"), entry("dup"), entry("dup"), entry("b")))
        installCache(cache)

        val codes = DictItemCodeFinder().getDictItemCodes("svc-1", "type-1")
        assertEquals(setOf("a", "dup", "b"), codes)
    }

    @Test
    fun getDictItemCodes_emptyWhenNoItems() {
        val cache = mock(SysDictItemHashCache::class.java)
        whenCalled(cache.getDictItems("svc-empty", "type-empty")).thenReturn(emptyList())
        installCache(cache)

        assertTrue(DictItemCodeFinder().getDictItemCodes("svc-empty", "type-empty").isEmpty())
    }

    @Test
    fun getDictItemCodes_resolvesBeanOnlyOnce() {
        val cache = mock(SysDictItemHashCache::class.java)
        whenCalled(cache.getDictItems("svc-1", "type-1")).thenReturn(listOf(entry("a")))
        val ctx = installCache(cache)

        val finder = DictItemCodeFinder()
        finder.getDictItemCodes("svc-1", "type-1")
        finder.getDictItemCodes("svc-1", "type-1")

        // bean resolution is cached after first call: context queried exactly once
        org.mockito.Mockito.verify(ctx, org.mockito.Mockito.times(1)).getBean(SysDictItemHashCache::class.java)
    }
}
