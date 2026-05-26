package io.kudos.ability.cache.common.init.properties

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [CacheVersionConfig] naming transformations.
 *
 * Core contract:
 *  - `getFinalCacheName("USER")` should return `<version>::USER`.
 *  - `getRealCacheName("v2::USER")` should strip back to `USER`.
 *  - When the version is blank, [CacheVersionConfig.getFinalCacheName] returns the original name (without `::`).
 *  - `getRealCacheName` returns names with non-matching prefixes unchanged — back-compat with legacy unprefixed data.
 *  - When the version is blank, [CacheVersionConfig.getRealCacheName] should not strip any prefix from any string.
 */
internal class CacheVersionConfigTest {

    @Test
    fun getFinalCacheName_addsVersionPrefix() {
        val cfg = CacheVersionConfig().apply { cacheVersion = "v2" }
        assertEquals("v2::USER", cfg.getFinalCacheName("USER"))
    }

    @Test
    fun getFinalCacheName_blankVersion_returnsAsIs() {
        val cfg = CacheVersionConfig().apply { cacheVersion = "" }
        assertEquals("USER", cfg.getFinalCacheName("USER"))
    }

    @Test
    fun getFinalCacheName_whitespaceVersion_returnsAsIs() {
        val cfg = CacheVersionConfig().apply { cacheVersion = "   " }
        assertEquals("USER", cfg.getFinalCacheName("USER"))
    }

    @Test
    fun getRealCacheName_stripsMatchingPrefix() {
        val cfg = CacheVersionConfig().apply { cacheVersion = "v2" }
        assertEquals("USER", cfg.getRealCacheName("v2::USER"))
    }

    @Test
    fun getRealCacheName_unprefixed_returnsAsIs() {
        val cfg = CacheVersionConfig().apply { cacheVersion = "v2" }
        // Back-compat with legacy unprefixed data: returns names with non-matching prefixes unchanged.
        assertEquals("USER", cfg.getRealCacheName("USER"))
    }

    @Test
    fun getRealCacheName_differentVersionPrefix_returnsAsIs() {
        val cfg = CacheVersionConfig().apply { cacheVersion = "v3" }
        // Not the prefix of the current version — per contract, returned unchanged (business code is responsible for distinguishing old vs. new data).
        assertEquals("v2::USER", cfg.getRealCacheName("v2::USER"))
    }

    @Test
    fun realMsgChannel_includesVersion() {
        val cfg = CacheVersionConfig().apply { cacheVersion = "v2" }
        assertEquals("v2:cache:local-remote:channel", cfg.realMsgChannel)
    }
}
