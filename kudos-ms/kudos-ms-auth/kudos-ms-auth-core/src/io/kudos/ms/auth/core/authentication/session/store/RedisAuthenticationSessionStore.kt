package io.kudos.ms.auth.core.authentication.session.store

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import org.springframework.data.redis.connection.ReturnType
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import java.security.MessageDigest

/** Redis-backed, multi-node authentication-session metadata store with atomic CAS updates. */
open class RedisAuthenticationSessionStore(
    redisTemplates: RedisTemplates,
) : IAuthenticationSessionStore {

    private val template = redisTemplates.defaultRedisTemplate
    private val serializer = JdkSerializationRedisSerializer()

    override fun create(session: AuthenticationSession): Boolean {
        val result = template.execute(RedisCallback<Long> { connection ->
            connection.scriptingCommands().eval(
                CREATE_SCRIPT,
                ReturnType.INTEGER,
                2,
                key(session.id),
                userIndexKey(session.tenantId, session.userId),
                VERSION_FIELD,
                session.version.toString().bytes(),
                PAYLOAD_FIELD,
                requireNotNull(serializer.serialize(session)),
                session.absoluteExpiresAt.toEpochMilli().toString().bytes(),
                session.id.bytes(),
            )
        })
        return result == 1L
    }

    override fun get(id: String): AuthenticationSession? =
        template.execute(RedisCallback<AuthenticationSession?> { connection ->
            connection.hashCommands().hGet(key(id), PAYLOAD_FIELD)
                ?.let(serializer::deserialize)
                as? AuthenticationSession
        })

    override fun findByUser(tenantId: String, userId: String): List<AuthenticationSession> =
        template.execute(RedisCallback<List<AuthenticationSession>> { connection ->
            val indexKey = userIndexKey(tenantId, userId)
            val sessionIds = connection.setCommands().sMembers(indexKey).orEmpty()
            val staleIds = mutableListOf<ByteArray>()
            val sessions = sessionIds.mapNotNull { encodedId ->
                val id = encodedId.toString(Charsets.UTF_8)
                val session = connection.hashCommands().hGet(key(id), PAYLOAD_FIELD)
                    ?.let(serializer::deserialize)
                    as? AuthenticationSession
                session
                    ?.takeIf { it.tenantId == tenantId && it.userId == userId }
                    ?: run {
                        staleIds += encodedId
                        null
                    }
            }
            if (staleIds.isNotEmpty()) {
                connection.setCommands().sRem(indexKey, *staleIds.toTypedArray())
            }
            sessions
        }).orEmpty()

    override fun save(session: AuthenticationSession, expectedVersion: Long): AuthenticationSession? {
        val versioned = session.copy(version = expectedVersion + 1)
        val result = template.execute(RedisCallback<Long> { connection ->
            connection.scriptingCommands().eval(
                SAVE_SCRIPT,
                ReturnType.INTEGER,
                1,
                key(session.id),
                VERSION_FIELD,
                expectedVersion.toString().bytes(),
                versioned.version.toString().bytes(),
                PAYLOAD_FIELD,
                requireNotNull(serializer.serialize(versioned)),
                versioned.absoluteExpiresAt.toEpochMilli().toString().bytes(),
            )
        })
        return versioned.takeIf { result == 1L }
    }

    private fun key(id: String): ByteArray = "$KEY_PREFIX$id".bytes()

    private fun userIndexKey(tenantId: String, userId: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$tenantId\u0000$userId".bytes())
            .joinToString("") { "%02x".format(it) }
        return "$USER_INDEX_KEY_PREFIX$digest".bytes()
    }

    private fun String.bytes(): ByteArray = toByteArray(Charsets.UTF_8)

    companion object {
        private const val KEY_PREFIX = "kudos:auth:session:"
        private const val USER_INDEX_KEY_PREFIX = "kudos:auth:session:user:"
        private val VERSION_FIELD = "version".toByteArray(Charsets.UTF_8)
        private val PAYLOAD_FIELD = "payload".toByteArray(Charsets.UTF_8)

        private val CREATE_SCRIPT = """
            if redis.call('EXISTS', KEYS[1]) == 1 then
                return 0
            end
            redis.call('HSET', KEYS[1], ARGV[1], ARGV[2], ARGV[3], ARGV[4])
            redis.call('PEXPIREAT', KEYS[1], ARGV[5])
            redis.call('SADD', KEYS[2], ARGV[6])
            local redisTime = redis.call('TIME')
            local nowMillis = redisTime[1] * 1000 + math.floor(redisTime[2] / 1000)
            local requestedTtl = tonumber(ARGV[5]) - nowMillis
            local currentTtl = redis.call('PTTL', KEYS[2])
            if requestedTtl > 0 and (currentTtl < 0 or requestedTtl > currentTtl) then
                redis.call('PEXPIRE', KEYS[2], requestedTtl)
            end
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
