package io.kudos.ability.data.memdb.redis.consts

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [CacheKey] assembly rules.
 *
 * Does not require a Redis container; regression protection for pure string concatenation logic.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
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
