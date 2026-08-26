package io.kudos.ms.auth.provider.oauth2.authorization

import io.kudos.ability.data.memdb.redis.RedisTemplates
import org.springframework.data.redis.connection.ReturnType
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import java.security.MessageDigest
import java.time.Instant

/** Redis storage with TTL, collision protection and atomic callback consumption. */
open class RedisExternalAuthorizationRequestStore(
    redisTemplates: RedisTemplates,
) : IExternalAuthorizationRequestStore {
    private val template = redisTemplates.defaultRedisTemplate
    private val serializer = JdkSerializationRedisSerializer()

    override fun create(
        authorizationRequest: OAuth2AuthorizationRequest,
        expiresAt: Instant,
    ): Boolean {
        val state = requireNotNull(authorizationRequest.state) { "OAuth authorization state is required" }
        val result = template.execute(RedisCallback<Long> { connection ->
            connection.scriptingCommands().eval(
                CREATE_SCRIPT,
                ReturnType.INTEGER,
                1,
                key(state),
                requireNotNull(serializer.serialize(authorizationRequest)),
                expiresAt.toEpochMilli().toString().bytes(),
            )
        })
        return result == 1L
    }

    override fun get(state: String): OAuth2AuthorizationRequest? {
        val bytes = template.execute(RedisCallback<ByteArray?> { connection ->
            connection.stringCommands().get(key(state))
        }) ?: return null
        return serializer.deserialize(bytes) as? OAuth2AuthorizationRequest
    }

    override fun consume(state: String): OAuth2AuthorizationRequest? {
        val bytes = template.execute(RedisCallback<ByteArray?> { connection ->
            connection.scriptingCommands().eval(CONSUME_SCRIPT, ReturnType.VALUE, 1, key(state))
        }) ?: return null
        return serializer.deserialize(bytes) as? OAuth2AuthorizationRequest
    }

    override fun delete(state: String) {
        template.execute(RedisCallback<Long> { connection -> connection.keyCommands().del(key(state)) })
    }

    private fun key(state: String): ByteArray = "$KEY_PREFIX${digest(state)}".bytes()

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.bytes())
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun String.bytes() = toByteArray(Charsets.UTF_8)

    private companion object {
        const val KEY_PREFIX = "kudos:auth:oauth2-authorization-request:"

        val CREATE_SCRIPT = """
            if redis.call('EXISTS', KEYS[1]) == 1 then
                return 0
            end
            redis.call('SET', KEYS[1], ARGV[1])
            redis.call('PEXPIREAT', KEYS[1], ARGV[2])
            return 1
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val CONSUME_SCRIPT = """
            local value = redis.call('GET', KEYS[1])
            if value then
                redis.call('DEL', KEYS[1])
            end
            return value
        """.trimIndent().toByteArray(Charsets.UTF_8)
    }
}
