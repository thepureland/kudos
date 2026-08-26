package io.kudos.ms.user.core.passport.security

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Real Redis verification for multi-bucket atomic authentication attempt limiting. */
@EnabledIfDockerInstalled
internal class RedisAuthenticationAttemptStoreTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var redisTemplates: RedisTemplates

    @Resource
    private lateinit var configuredStore: IAuthenticationAttemptStore

    @Test
    fun configuredRedisStoreAtomicallyConsumesAndClearsAllBuckets() {
        assertIs<RedisAuthenticationAttemptStore>(configuredStore)
        val store = RedisAuthenticationAttemptStore(redisTemplates)
        val suffix = UUID.randomUUID().toString()
        val strict = AuthenticationAttemptBucket("test:auth-attempt:strict:$suffix", 1, 60)
        val relaxed = AuthenticationAttemptBucket("test:auth-attempt:relaxed:$suffix", 2, 60)

        try {
            assertTrue(store.consume(listOf(strict, relaxed)).allowed)

            val blocked = store.consume(listOf(strict, relaxed))
            assertFalse(blocked.allowed)
            assertTrue(assertNotNull(blocked.retryAfterSeconds) in 1..60)

            // The rejected multi-bucket operation must not consume the relaxed bucket.
            store.clear(listOf(strict.key))
            assertTrue(store.consume(listOf(strict, relaxed)).allowed)
            assertFalse(store.inspect(listOf(relaxed)).allowed)

            store.clear(listOf(strict.key, relaxed.key))
            assertEquals(AuthenticationAttemptStoreDecision.ALLOWED, store.inspect(listOf(strict, relaxed)))
        } finally {
            store.clear(listOf(strict.key, relaxed.key))
        }
    }
}
