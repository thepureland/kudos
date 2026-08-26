package io.kudos.ms.auth.core.authentication.mfa.store

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ms.auth.core.authentication.mfa.TotpEnrollment
import org.springframework.data.redis.connection.ReturnType
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer

/** Redis-backed pending TOTP enrollment store with atomic CAS and one-time consumption. */
open class RedisTotpEnrollmentStore(
    redisTemplates: RedisTemplates,
) : ITotpEnrollmentStore {

    private val template = redisTemplates.defaultRedisTemplate
    private val serializer = JdkSerializationRedisSerializer()

    override fun create(enrollment: TotpEnrollment): Boolean =
        template.execute(RedisCallback<Long> { connection ->
            connection.scriptingCommands().eval(
                CREATE_SCRIPT,
                ReturnType.INTEGER,
                1,
                key(enrollment.id),
                VERSION_FIELD,
                enrollment.version.toString().bytes(),
                PAYLOAD_FIELD,
                requireNotNull(serializer.serialize(enrollment)),
                enrollment.expiresAt.toEpochMilli().toString().bytes(),
            )
        }) == 1L

    override fun get(id: String): TotpEnrollment? =
        template.execute(RedisCallback<TotpEnrollment?> { connection ->
            connection.hashCommands().hGet(key(id), PAYLOAD_FIELD)
                ?.let(serializer::deserialize) as? TotpEnrollment
        })

    override fun save(enrollment: TotpEnrollment, expectedVersion: Long): TotpEnrollment? {
        val versioned = enrollment.copy(version = expectedVersion + 1)
        val result = template.execute(RedisCallback<Long> { connection ->
            connection.scriptingCommands().eval(
                SAVE_SCRIPT,
                ReturnType.INTEGER,
                1,
                key(enrollment.id),
                VERSION_FIELD,
                expectedVersion.toString().bytes(),
                versioned.version.toString().bytes(),
                PAYLOAD_FIELD,
                requireNotNull(serializer.serialize(versioned)),
                versioned.expiresAt.toEpochMilli().toString().bytes(),
            )
        })
        return versioned.takeIf { result == 1L }
    }

    override fun consume(id: String, expectedVersion: Long): TotpEnrollment? {
        val payload = template.execute(RedisCallback<ByteArray?> { connection ->
            connection.scriptingCommands().eval(
                CONSUME_SCRIPT,
                ReturnType.VALUE,
                1,
                key(id),
                VERSION_FIELD,
                expectedVersion.toString().bytes(),
                PAYLOAD_FIELD,
            ) as? ByteArray
        }) ?: return null
        return serializer.deserialize(payload) as? TotpEnrollment
    }

    private fun key(id: String) = "$KEY_PREFIX$id".bytes()
    private fun String.bytes() = toByteArray(Charsets.UTF_8)

    companion object {
        private const val KEY_PREFIX = "kudos:auth:totp-enrollment:"
        private val VERSION_FIELD = "version".toByteArray(Charsets.UTF_8)
        private val PAYLOAD_FIELD = "payload".toByteArray(Charsets.UTF_8)

        private val CREATE_SCRIPT = """
            if redis.call('EXISTS', KEYS[1]) == 1 then return 0 end
            redis.call('HSET', KEYS[1], ARGV[1], ARGV[2], ARGV[3], ARGV[4])
            redis.call('PEXPIREAT', KEYS[1], ARGV[5])
            return 1
        """.trimIndent().toByteArray(Charsets.UTF_8)

        private val SAVE_SCRIPT = """
            local current = redis.call('HGET', KEYS[1], ARGV[1])
            if not current or current ~= ARGV[2] then return 0 end
            redis.call('HSET', KEYS[1], ARGV[1], ARGV[3], ARGV[4], ARGV[5])
            redis.call('PEXPIREAT', KEYS[1], ARGV[6])
            return 1
        """.trimIndent().toByteArray(Charsets.UTF_8)

        private val CONSUME_SCRIPT = """
            local current = redis.call('HGET', KEYS[1], ARGV[1])
            if not current or current ~= ARGV[2] then return nil end
            local payload = redis.call('HGET', KEYS[1], ARGV[3])
            redis.call('DEL', KEYS[1])
            return payload
        """.trimIndent().toByteArray(Charsets.UTF_8)
    }
}
