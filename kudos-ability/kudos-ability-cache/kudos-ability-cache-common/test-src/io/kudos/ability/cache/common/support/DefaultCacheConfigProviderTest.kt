package io.kudos.ability.cache.common.support

import io.kudos.ability.cache.common.init.properties.CacheItemsProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [DefaultCacheConfigProvider] yml 字符串解析 + 按策略分组的单测。
 *
 * 覆盖：
 *  - 基本字段解析：`name=X&strategy=Y&ttl=Z` 三件套
 *  - 按 [io.kudos.ability.cache.common.enums.CacheStrategy] 分组：SINGLE_LOCAL / REMOTE / LOCAL_REMOTE
 *    各自的视图正确切分
 *  - `hash=true` 进入 hashCacheConfigs 视图
 *  - 缺 strategy → 抛错（README 文档化的解析期校验）
 *  - 空白行 / 空列表的安全处理
 *  - resolvedStrategyCode 兜底（仅 strategyDictCode 也能分组）—— 守护 round-5
 *    迁移的 raw-reader → derived prop 改动
 */
internal class DefaultCacheConfigProviderTest {

    @Test
    fun parsesNameStrategyTtl() {
        val provider = newProvider(
            "name=USER&strategy=LOCAL_REMOTE&ttl=900",
        )
        val cfg = provider.getCacheConfig("USER")!!
        assertEquals("USER", cfg.name)
        assertEquals("LOCAL_REMOTE", cfg.strategy)
        assertEquals(900, cfg.ttl)
    }

    @Test
    fun groupsByStrategy_partitionsCorrectly() {
        val provider = newProvider(
            "name=A&strategy=SINGLE_LOCAL",
            "name=B&strategy=REMOTE",
            "name=C&strategy=LOCAL_REMOTE",
            "name=D&strategy=LOCAL_REMOTE",
        )
        assertEquals(setOf("A"), provider.getLocalCacheConfigs().keys)
        assertEquals(setOf("B"), provider.getRemoteCacheConfigs().keys)
        assertEquals(setOf("C", "D"), provider.getLocalRemoteCacheConfigs().keys)
    }

    @Test
    fun flatView_aggregatesAllStrategies() {
        val provider = newProvider(
            "name=A&strategy=SINGLE_LOCAL",
            "name=B&strategy=REMOTE",
            "name=C&strategy=LOCAL_REMOTE",
        )
        assertEquals(setOf("A", "B", "C"), provider.getAllCacheConfigs().keys)
    }

    @Test
    fun hashView_filtersByHashFlag() {
        val provider = newProvider(
            "name=PLAIN&strategy=REMOTE",
            "name=DICT&strategy=REMOTE&hash=true",
        )
        // 仅 hash=true 的进入 hash 视图
        assertEquals(setOf("DICT"), provider.getHashCacheConfigs().keys)
        // 但仍出现在 all / strategy 视图
        assertEquals(setOf("PLAIN", "DICT"), provider.getAllCacheConfigs().keys)
        assertEquals(setOf("PLAIN", "DICT"), provider.getRemoteCacheConfigs().keys)
    }

    @Test
    fun missingStrategy_throwsAtInit() {
        // 解析期校验：缺 strategy / strategyDictCode 应当快速失败
        val ex = assertFails { newProvider("name=USER&ttl=900") }
        assertTrue(ex.message?.contains("cache item 缺少 strategy") == true,
            "错误信息应明确指出缺 strategy，实际：${ex.message}")
    }

    @Test
    fun resolvedStrategyCode_groupsByDictCode_whenStrategyMissing() {
        // 即使 yml 配置只填 strategyDictCode（DB 字典码路径），分组也应按 derived prop 工作
        val provider = newProvider("name=DICT_ONLY&strategyDictCode=LOCAL_REMOTE")
        assertEquals(setOf("DICT_ONLY"), provider.getLocalRemoteCacheConfigs().keys)
    }

    @Test
    fun blankLines_skipped() {
        // 空白行不应导致 NPE / 解析错误
        val provider = newProvider(
            "name=A&strategy=REMOTE",
            "   ",
            "",
            "name=B&strategy=REMOTE",
        )
        assertEquals(setOf("A", "B"), provider.getRemoteCacheConfigs().keys)
    }

    @Test
    fun emptyItems_yieldsEmptyProvider() {
        val provider = newProvider()
        assertTrue(provider.getAllCacheConfigs().isEmpty())
        assertTrue(provider.getLocalCacheConfigs().isEmpty())
        assertTrue(provider.getRemoteCacheConfigs().isEmpty())
        assertTrue(provider.getLocalRemoteCacheConfigs().isEmpty())
        assertTrue(provider.getHashCacheConfigs().isEmpty())
    }

    @Test
    fun unknownCacheName_returnsNull() {
        val provider = newProvider("name=A&strategy=REMOTE")
        assertNull(provider.getCacheConfig("does-not-exist"))
    }

    @Test
    fun writeOnBootDefaultsToFalse_butExplicitTrueIsHonored() {
        val provider = newProvider(
            "name=DEFAULT&strategy=REMOTE",
            "name=BOOT&strategy=REMOTE&writeOnBoot=true",
        )
        assertEquals(false, provider.getCacheConfig("DEFAULT")?.isWriteOnBoot)
        assertEquals(true, provider.getCacheConfig("BOOT")?.isWriteOnBoot)
    }

    private fun newProvider(vararg items: String): DefaultCacheConfigProvider {
        val props = CacheItemsProperties().apply { cacheItems = items.toMutableList() }
        return DefaultCacheConfigProvider(props)
    }
}
