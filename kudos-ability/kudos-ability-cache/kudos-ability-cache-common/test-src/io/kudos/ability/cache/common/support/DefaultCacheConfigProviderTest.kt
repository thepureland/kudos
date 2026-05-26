package io.kudos.ability.cache.common.support

import io.kudos.ability.cache.common.init.properties.CacheItemsProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [DefaultCacheConfigProvider] yml string parsing and grouping by strategy.
 *
 * Coverage:
 *  - Basic field parsing: the `name=X&strategy=Y&ttl=Z` triplet.
 *  - Grouping by [io.kudos.ability.cache.common.enums.CacheStrategy]: SINGLE_LOCAL / REMOTE / LOCAL_REMOTE
 *    views are partitioned correctly.
 *  - `hash=true` lands in the hashCacheConfigs view.
 *  - Missing strategy → throws (parse-time validation documented in the README).
 *  - Safe handling of blank lines / empty lists.
 *  - resolvedStrategyCode fallback (grouping works with only strategyDictCode) — guards the round-5
 *    raw-reader → derived prop migration.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
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
        // Only entries with hash=true land in the hash view.
        assertEquals(setOf("DICT"), provider.getHashCacheConfigs().keys)
        // They still appear in the all / strategy views.
        assertEquals(setOf("PLAIN", "DICT"), provider.getAllCacheConfigs().keys)
        assertEquals(setOf("PLAIN", "DICT"), provider.getRemoteCacheConfigs().keys)
    }

    @Test
    fun missingStrategy_throwsAtInit() {
        // Parse-time validation: missing strategy / strategyDictCode should fail fast.
        val ex = assertFails { newProvider("name=USER&ttl=900") }
        assertTrue(ex.message?.contains("cache item is missing strategy") == true,
            "Error message should clearly indicate the missing strategy; actual: ${ex.message}")
    }

    @Test
    fun resolvedStrategyCode_groupsByDictCode_whenStrategyMissing() {
        // Even when yml only sets strategyDictCode (the DB dict-code path), grouping should work via the derived prop.
        val provider = newProvider("name=DICT_ONLY&strategyDictCode=LOCAL_REMOTE")
        assertEquals(setOf("DICT_ONLY"), provider.getLocalRemoteCacheConfigs().keys)
    }

    @Test
    fun blankLines_skipped() {
        // Blank lines must not cause NPE or parse errors.
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

    @Test
    fun structuredCacheItemConfigs_areLoadedWithSameDefaults() {
        val props = CacheItemsProperties().apply {
            cacheItemConfigs = mutableListOf(
                CacheConfig().apply {
                    name = "STRUCTURED"
                    strategy = "LOCAL_REMOTE"
                    ttl = 60
                }
            )
        }

        val provider = DefaultCacheConfigProvider(props)

        val cfg = provider.getCacheConfig("STRUCTURED")!!
        assertEquals("LOCAL_REMOTE", cfg.resolvedStrategyCode)
        assertEquals(60, cfg.ttl)
        assertEquals(false, cfg.isWriteOnBoot)
        assertEquals(true, cfg.isActive)
        assertEquals(setOf("STRUCTURED"), provider.getLocalRemoteCacheConfigs().keys)
    }

    @Test
    fun stringCacheItem_unknownFieldFailsFast() {
        val ex = assertFails { newProvider("name=USER&strategy=REMOTE&ttle=900") }
        assertTrue(
            ex.message?.contains("unknown field 'ttle'") == true,
            "Error message should clearly indicate the unknown field; actual: ${ex.message}"
        )
    }

    @Test
    fun stringCacheItem_malformedTokenFailsFast() {
        val ex = assertFails { newProvider("name=USER&strategy=REMOTE&ttl") }
        assertTrue(
            ex.message?.contains("parameter format is invalid") == true,
            "Error message should clearly indicate the parameter format error; actual: ${ex.message}"
        )
    }

    @Test
    fun invalidStrategy_failsFast() {
        val ex = assertFails { newProvider("name=USER&strategy=NOT_A_STRATEGY") }
        assertTrue(
            ex.message?.contains("strategy is invalid 'NOT_A_STRATEGY'") == true,
            "Error message should clearly indicate the invalid strategy; actual: ${ex.message}"
        )
    }

    private fun newProvider(vararg items: String): DefaultCacheConfigProvider {
        val props = CacheItemsProperties().apply { cacheItems = items.toMutableList() }
        return DefaultCacheConfigProvider(props)
    }
}
