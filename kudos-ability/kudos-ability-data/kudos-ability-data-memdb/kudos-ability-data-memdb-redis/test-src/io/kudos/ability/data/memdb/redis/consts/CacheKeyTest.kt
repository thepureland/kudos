package io.kudos.ability.data.memdb.redis.consts

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [CacheKey] 拼装规则的单元测试。
 *
 * 不需要 Redis 容器；纯字符串拼接逻辑的回归保护。
 */
internal class CacheKeyTest {

    @Test
    fun getCacheKey_joinsWithColon() {
        assertEquals("a:b:c", CacheKey.getCacheKey("a", "b", "c"))
    }

    @Test
    fun getCacheKey_emptyArgs_returnsEmpty() {
        assertEquals("", CacheKey.getCacheKey())
    }

    @Test
    fun getCacheKey_singleArg_returnsArg() {
        assertEquals("only", CacheKey.getCacheKey("only"))
    }

    @Test
    fun getCacheKeyPrefix_joinsWithComma() {
        assertEquals("1,2,3", CacheKey.getCacheKeyPrefix("1", "2", "3"))
    }

    @Test
    fun getCacheKeyPrefix_emptyArgs_returnsEmpty() {
        assertEquals("", CacheKey.getCacheKeyPrefix())
    }

    @Test
    fun separators_areTheExpectedConstants() {
        assertEquals(":", CacheKey.CACHE_KEY_SEPERATOR)
        assertEquals(",", CacheKey.CACHE_KEY_PREFIX_SEPERATOR)
    }
}
