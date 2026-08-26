package io.kudos.ms.auth.provider.oauth2.state

import io.kudos.ability.data.memdb.redis.RedisTemplates
import org.springframework.data.redis.connection.ReturnType
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer

/** Redis implementation with atomic create and consume semantics. */
open class RedisExternalLoginStateStore(
    redisTemplates: RedisTemplates,
) : IExternalLoginStateStore {

    private val template = redisTemplates.defaultRedisTemplate
    private val serializer = JdkSerializationRedisSerializer()

    override fun create(state: ExternalLoginState): Boolean {
        val result = template.execute(RedisCallback<Long> { connection ->
            connection.scriptingCommands().eval(
                CREATE_SCRIPT,
                ReturnType.INTEGER,
                1,
                key(state.state),
                requireNotNull(serializer.serialize(state)),
                state.expiresAt.toEpochMilli().toString().toByteArray(Charsets.UTF_8),
            )
        })
        return result == 1L
    }

    override fun consume(state: String): ExternalLoginState? {
        val bytes = template.execute(RedisCallback<ByteArray?> { connection ->
            connection.scriptingCommands().eval(
                CONSUME_SCRIPT,
                ReturnType.VALUE,
                1,
                key(state),
            )
        }) ?: return null
        return (serializer.deserialize(bytes) as? ExternalLoginState)
            ?.takeIf { java.time.Instant.now().isBefore(it.expiresAt) }
    }

    private fun key(state: String): ByteArray = "$KEY_PREFIX$state".toByteArray(Charsets.UTF_8)

    companion object {
        private const val KEY_PREFIX = "kudos:auth:external-login-state:"

        private val CREATE_SCRIPT = """
            if redis.call('EXISTS', KEYS[1]) == 1 then
                return 0
            end
            redis.call('SET', KEYS[1], ARGV[1])
            redis.call('PEXPIREAT', KEYS[1], ARGV[2])
            return 1
        """.trimIndent().toByteArray(Charsets.UTF_8)

        private val CONSUME_SCRIPT = """
            local value = redis.call('GET', KEYS[1])
            if value then
                redis.call('DEL', KEYS[1])
            end
            return value
        """.trimIndent().toByteArray(Charsets.UTF_8)
    }
}
