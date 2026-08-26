package io.kudos.ms.auth.provider.webauthn.ceremony

import io.kudos.ability.data.memdb.redis.RedisTemplates
import org.springframework.data.redis.connection.ReturnType
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import java.time.Clock

/** Multi-node challenge state with Redis-atomic creation and one-time consumption. */
open class RedisWebAuthnCeremonyStore(
    redisTemplates: RedisTemplates,
    private val clock: Clock = Clock.systemUTC(),
) : IWebAuthnCeremonyStore {
    private val template = redisTemplates.defaultRedisTemplate
    private val serializer = JdkSerializationRedisSerializer()

    override fun create(state: WebAuthnCeremonyState): Boolean {
        if (!clock.instant().isBefore(state.expiresAt)) return false
        return template.execute(RedisCallback<Long> { connection ->
            connection.scriptingCommands().eval(
                CREATE_SCRIPT,
                ReturnType.INTEGER,
                1,
                key(state.id),
                requireNotNull(serializer.serialize(state)),
                state.expiresAt.toEpochMilli().toString().bytes(),
            )
        }) == 1L
    }

    override fun get(id: String): WebAuthnCeremonyState? =
        template.execute(RedisCallback<WebAuthnCeremonyState?> { connection ->
            connection.stringCommands().get(key(id))?.let(serializer::deserialize) as? WebAuthnCeremonyState
        })?.takeIf { clock.instant().isBefore(it.expiresAt) }

    override fun consume(id: String): WebAuthnCeremonyState? {
        val payload = template.execute(RedisCallback<ByteArray?> { connection ->
            connection.scriptingCommands().eval(
                CONSUME_SCRIPT,
                ReturnType.VALUE,
                1,
                key(id),
            ) as? ByteArray
        }) ?: return null
        return (serializer.deserialize(payload) as? WebAuthnCeremonyState)
            ?.takeIf { clock.instant().isBefore(it.expiresAt) }
    }

    private fun key(id: String) = "$KEY_PREFIX$id".bytes()
    private fun String.bytes() = toByteArray(Charsets.UTF_8)

    private companion object {
        const val KEY_PREFIX = "kudos:auth:webauthn-ceremony:"
        val CREATE_SCRIPT = """
            if redis.call('EXISTS', KEYS[1]) == 1 then return 0 end
            redis.call('SET', KEYS[1], ARGV[1])
            redis.call('PEXPIREAT', KEYS[1], ARGV[2])
            return 1
        """.trimIndent().toByteArray(Charsets.UTF_8)
        val CONSUME_SCRIPT = """
            local value = redis.call('GET', KEYS[1])
            if value then redis.call('DEL', KEYS[1]) end
            return value
        """.trimIndent().toByteArray(Charsets.UTF_8)
    }
}
