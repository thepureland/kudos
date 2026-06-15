package io.kudos.ability.data.memdb.redis.aop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

/**
 * Unit tests for the [LimitType] enum and the [RateLimiter] annotation defaults.
 *
 * Pure unit test, no Redis required.
 *
 * @author K
 * @since 1.0.0
 */
internal class LimitTypeTest {

    @Test
    fun limitType_hasExactlyThreeStrategies() {
        assertEquals(listOf(LimitType.DEFAULT, LimitType.USER, LimitType.IP), LimitType.entries.toList())
    }

    @Test
    fun limitType_valueOf_roundTrip() {
        LimitType.entries.forEach { assertSame(it, LimitType.valueOf(it.name)) }
        assertFailsWith<IllegalArgumentException> { LimitType.valueOf("GLOBAL") }
    }

    @Test
    fun rateLimiter_defaults() {
        val annotation = RateLimiter()
        assertEquals(60, annotation.time)
        assertEquals(100, annotation.count)
        assertEquals(LimitType.DEFAULT, annotation.limitType)
    }

    @Test
    fun rateLimiter_customValues() {
        val annotation = RateLimiter(time = 5, count = 1, limitType = LimitType.IP)
        assertEquals(5, annotation.time)
        assertEquals(1, annotation.count)
        assertEquals(LimitType.IP, annotation.limitType)
    }
}
