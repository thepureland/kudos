package io.kudos.ability.cache.remote.redis.support

import io.kudos.ability.data.memdb.redis.RedisTemplates
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [RedisRemoteCacheProcessor].
 *
 * Covers all branches with mocked Redis collaborators:
 * - getCacheData hit / miss
 * - writeCacheData: null value no-op vs put + expire (millisecond duration)
 * - clearCache: blank cacheKey no-op, whole-cache delete (b=true), single-field delete (b=false, s non-blank),
 *   and the no-op branch (b=false, s blank)
 *
 * @author K
 * @since 1.0.0
 */
internal class RedisRemoteCacheProcessorTest {

    private lateinit var redisTemplates: RedisTemplates
    private lateinit var template: RedisTemplate<Any, Any?>
    private lateinit var hashOps: HashOperations<Any, Any?, Any?>
    private lateinit var processor: RedisRemoteCacheProcessor

    @Suppress("UNCHECKED_CAST")
    @BeforeTest
    fun setUp() {
        template = mock(RedisTemplate::class.java) as RedisTemplate<Any, Any?>
        hashOps = mock(HashOperations::class.java) as HashOperations<Any, Any?, Any?>
        `when`(template.opsForHash<Any?, Any?>()).thenReturn(hashOps)
        redisTemplates = RedisTemplates(mutableMapOf("default" to template), template)
        processor = RedisRemoteCacheProcessor(redisTemplates)
    }

    private fun verifyNoDeleteOrExpire() {
        verify(template, never()).delete(ArgumentMatchers.any<Any>())
        verify(template, never()).expire(ArgumentMatchers.any(), ArgumentMatchers.any<Duration>())
    }

    @Test
    fun getCacheData_returnsHashValue() {
        `when`(hashOps.get("ck", "dk")).thenReturn("value")
        assertEquals("value", processor.getCacheData("ck", "dk"))
    }

    @Test
    fun getCacheData_returnsNullWhenAbsent() {
        `when`(hashOps.get("ck", "missing")).thenReturn(null)
        assertNull(processor.getCacheData("ck", "missing"))
    }

    @Test
    fun writeCacheData_nullValue_isNoOp() {
        processor.writeCacheData("ck", "dk", null, 1000L)
        verifyNoInteractions(hashOps)
        verifyNoDeleteOrExpire()
    }

    @Test
    fun writeCacheData_putsValueAndExpiresWholeHashInMillis() {
        processor.writeCacheData("ck", "dk", "v", 1500L)
        verify(hashOps).put("ck", "dk", "v")
        verify(template).expire("ck", Duration.ofMillis(1500L))
    }

    @Test
    fun writeCacheData_zeroTimeout_stillExpires() {
        processor.writeCacheData("ck", "dk", "v", 0L)
        verify(hashOps).put("ck", "dk", "v")
        verify(template).expire("ck", Duration.ofMillis(0L))
    }

    @Test
    fun clearCache_blankCacheKey_isNoOp() {
        processor.clearCache("", "s", true)
        processor.clearCache("   ", "s", false)
        verifyNoInteractions(hashOps)
        verifyNoDeleteOrExpire()
    }

    @Test
    fun clearCache_wholeCache_deletesHashKey() {
        processor.clearCache("ck", "ignored", true)
        verify(template).delete("ck")
        verifyNoInteractions(hashOps)
    }

    @Test
    fun clearCache_singleEntry_deletesHashField() {
        processor.clearCache("ck", "field", false)
        verify(hashOps).delete("ck", "field")
        verify(template, never()).delete(ArgumentMatchers.any<Any>())
    }

    @Test
    fun clearCache_blankEntryKey_isNoOp() {
        processor.clearCache("ck", " ", false)
        verifyNoInteractions(hashOps)
        verifyNoDeleteOrExpire()
    }
}
