package io.kudos.ms.auth.core.authentication.store

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import org.springframework.data.redis.connection.ReturnType
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer

/**
 * Redis-backed authentication transaction store.
 *
 * A dedicated binary serializer is used instead of the application's configurable Redis value
 * serializer so deployments can change their general cache format without invalidating this
 * protocol. Lua keeps create and compare-and-set updates atomic across service instances.
 */
open class RedisAuthenticationTransactionStore(
    redisTemplates: RedisTemplates,
) : IAuthenticationTransactionStore {

    private val template = redisTemplates.defaultRedisTemplate
    private val serializer = JdkSerializationRedisSerializer()

    override fun create(transaction: AuthenticationTransaction): Boolean {
        val result = template.execute(RedisCallback<Long> { connection ->
            connection.scriptingCommands().eval(
                CREATE_SCRIPT,
                ReturnType.INTEGER,
                1,
                key(transaction.id),
                VERSION_FIELD,
                transaction.version.toString().bytes(),
                PAYLOAD_FIELD,
                requireNotNull(serializer.serialize(transaction)),
                transaction.expiresAt.toEpochMilli().toString().bytes(),
            )
        })
        return result == 1L
    }

    override fun get(id: String): AuthenticationTransaction? =
        template.execute(RedisCallback<AuthenticationTransaction?> { connection ->
            connection.hashCommands().hGet(key(id), PAYLOAD_FIELD)
                ?.let(serializer::deserialize)
                as? AuthenticationTransaction
        })

    override fun save(
        transaction: AuthenticationTransaction,
        expectedVersion: Long,
    ): AuthenticationTransaction? {
        val versioned = transaction.copy(version = expectedVersion + 1)
        val result = template.execute(RedisCallback<Long> { connection ->
            connection.scriptingCommands().eval(
                SAVE_SCRIPT,
                ReturnType.INTEGER,
                1,
                key(transaction.id),
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

    private fun key(id: String): ByteArray = "$KEY_PREFIX$id".bytes()

    private fun String.bytes(): ByteArray = toByteArray(Charsets.UTF_8)

    companion object {
        private const val KEY_PREFIX = "kudos:auth:authentication-transaction:"
        private val VERSION_FIELD = "version".toByteArray(Charsets.UTF_8)
        private val PAYLOAD_FIELD = "payload".toByteArray(Charsets.UTF_8)

        private val CREATE_SCRIPT = """
            if redis.call('EXISTS', KEYS[1]) == 1 then
                return 0
            end
            redis.call('HSET', KEYS[1], ARGV[1], ARGV[2], ARGV[3], ARGV[4])
            redis.call('PEXPIREAT', KEYS[1], ARGV[5])
            return 1
        """.trimIndent().toByteArray(Charsets.UTF_8)

        private val SAVE_SCRIPT = """
            local current = redis.call('HGET', KEYS[1], ARGV[1])
            if not current or current ~= ARGV[2] then
                return 0
            end
            redis.call('HSET', KEYS[1], ARGV[1], ARGV[3], ARGV[4], ARGV[5])
            redis.call('PEXPIREAT', KEYS[1], ARGV[6])
            return 1
        """.trimIndent().toByteArray(Charsets.UTF_8)
    }
}
