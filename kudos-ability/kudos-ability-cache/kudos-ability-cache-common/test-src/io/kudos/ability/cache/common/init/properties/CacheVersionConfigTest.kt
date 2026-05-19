package io.kudos.ability.cache.common.init.properties

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [CacheVersionConfig] 命名变换的单元测试。
 *
 * 核心契约：
 *  - `getFinalCacheName("USER")` 应返回 `<version>::USER`
 *  - `getRealCacheName("v2::USER")` 应反向剥回 `USER`
 *  - 版本为空串时 [CacheVersionConfig.getFinalCacheName] 返回原名（不加 `::`）
 *  - `getRealCacheName` 对前缀不匹配的名字原样返回——兼容历史无前缀数据
 *  - 版本为空串时 [CacheVersionConfig.getRealCacheName] 对任何字符串都不应剥前缀
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
        // 兼容历史无前缀数据：前缀不匹配时原样返回
        assertEquals("USER", cfg.getRealCacheName("USER"))
    }

    @Test
    fun getRealCacheName_differentVersionPrefix_returnsAsIs() {
        val cfg = CacheVersionConfig().apply { cacheVersion = "v3" }
        // 不是当前 version 的前缀——按合约原样返回（业务侧负责区分新老数据）
        assertEquals("v2::USER", cfg.getRealCacheName("v2::USER"))
    }

    @Test
    fun realMsgChannel_includesVersion() {
        val cfg = CacheVersionConfig().apply { cacheVersion = "v2" }
        assertEquals("v2:cache:local-remote:channel", cfg.realMsgChannel)
    }
}
