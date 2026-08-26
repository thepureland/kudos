package io.kudos.ms.user.core.passport.security

import io.kudos.ability.data.memdb.redis.RedisTemplates
import org.springframework.data.redis.connection.ReturnType
import org.springframework.data.redis.core.RedisCallback

/** Multi-node atomic fixed-window store backed by the default Kudos Redis instance. */
open class RedisAuthenticationAttemptStore(
    redisTemplates: RedisTemplates,
) : IAuthenticationAttemptStore {
    private val template = redisTemplates.defaultRedisTemplate

    override fun inspect(buckets: List<AuthenticationAttemptBucket>): AuthenticationAttemptStoreDecision =
        evaluate(buckets, consume = false)

    override fun consume(buckets: List<AuthenticationAttemptBucket>): AuthenticationAttemptStoreDecision =
        evaluate(buckets, consume = true)

    override fun clear(keys: Collection<String>) {
        if (keys.isEmpty()) return
        template.execute(RedisCallback<Long> { connection ->
            connection.keyCommands().del(*keys.map { it.bytes() }.toTypedArray())
        })
    }

    private fun evaluate(
        buckets: List<AuthenticationAttemptBucket>,
        consume: Boolean,
    ): AuthenticationAttemptStoreDecision {
        if (buckets.isEmpty()) return AuthenticationAttemptStoreDecision.ALLOWED
        buckets.forEach(::validate)
        val keysAndArgs = ArrayList<ByteArray>(buckets.size * 3 + 1)
        keysAndArgs += buckets.map { it.key.bytes() }
        buckets.forEach { bucket ->
            keysAndArgs += bucket.maxAttempts.toString().bytes()
            keysAndArgs += bucket.windowSeconds.toString().bytes()
        }
        keysAndArgs += if (consume) ONE else ZERO
        val result = template.execute(RedisCallback<List<*>?> { connection ->
            connection.scriptingCommands().eval(
                EVALUATE_SCRIPT,
                ReturnType.MULTI,
                buckets.size,
                *keysAndArgs.toTypedArray(),
            ) as? List<*>
        }) ?: error("Authentication attempt Redis script returned no result")
        val allowed = result.getOrNull(0).longValue() == 1L
        val retryMillis = result.getOrNull(1).longValue().coerceAtLeast(0)
        return if (allowed) {
            AuthenticationAttemptStoreDecision.ALLOWED
        } else {
            AuthenticationAttemptStoreDecision(
                allowed = false,
                retryAfterSeconds = (retryMillis.coerceAtLeast(1) + 999) / 1000,
            )
        }
    }

    private fun validate(bucket: AuthenticationAttemptBucket) {
        require(bucket.key.isNotBlank()) { "Authentication attempt key must not be blank" }
        require(bucket.maxAttempts in 1..MAX_ATTEMPTS) { "Invalid authentication max attempts" }
        require(bucket.windowSeconds in 1..MAX_WINDOW_SECONDS) { "Invalid authentication window" }
    }

    private fun Any?.longValue(): Long = when (this) {
        is Number -> toLong()
        is ByteArray -> toString(Charsets.UTF_8).toLong()
        is String -> toLong()
        else -> error("Unexpected authentication attempt Redis result")
    }

    private fun String.bytes(): ByteArray = toByteArray(Charsets.UTF_8)

    private companion object {
        const val MAX_ATTEMPTS = 100_000
        const val MAX_WINDOW_SECONDS = 604_800L
        val ZERO = "0".toByteArray(Charsets.UTF_8)
        val ONE = "1".toByteArray(Charsets.UTF_8)

        /** Checks every bucket first, then consumes all buckets in one Redis operation. */
        val EVALUATE_SCRIPT = """
            local blocked_ttl = 0
            for i = 1, #KEYS do
                local count = tonumber(redis.call('GET', KEYS[i]) or '0')
                local limit = tonumber(ARGV[(i - 1) * 2 + 1])
                if count >= limit then
                    local ttl = redis.call('PTTL', KEYS[i])
                    if ttl < 1 then ttl = 1000 end
                    if ttl > blocked_ttl then blocked_ttl = ttl end
                end
            end
            if blocked_ttl > 0 then return {0, blocked_ttl} end
            if ARGV[#KEYS * 2 + 1] == '1' then
                for i = 1, #KEYS do
                    local count = redis.call('INCR', KEYS[i])
                    if count == 1 then
                        redis.call('EXPIRE', KEYS[i], tonumber(ARGV[(i - 1) * 2 + 2]))
                    end
                end
            end
            return {1, 0}
        """.trimIndent().toByteArray(Charsets.UTF_8)
    }
}
