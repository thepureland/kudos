package io.kudos.ms.auth.provider.emailotp.store

import io.kudos.ability.data.memdb.redis.RedisTemplates
import org.springframework.data.redis.connection.ReturnType
import org.springframework.data.redis.core.RedisCallback

/** Redis-backed, atomically consumed email OTP challenge store. */
open class RedisEmailOtpChallengeStore(
    redisTemplates: RedisTemplates,
) : IEmailOtpChallengeStore {
    private val template = redisTemplates.defaultRedisTemplate

    override fun issue(challenge: EmailOtpChallenge): Boolean {
        val result = template.execute(RedisCallback<Long> { connection ->
            connection.scriptingCommands().eval(
                ISSUE_SCRIPT,
                ReturnType.INTEGER,
                1,
                key(challenge.transactionId),
                TENANT_FIELD,
                challenge.tenantDigest.bytes(),
                EMAIL_FIELD,
                challenge.emailDigest.bytes(),
                CODE_FIELD,
                challenge.codeDigest.bytes(),
                FAILURES_FIELD,
                ZERO,
                MAX_ATTEMPTS_FIELD,
                challenge.maxAttempts.toString().bytes(),
                EXPIRES_AT_FIELD,
                challenge.expiresAt.toEpochMilli().toString().bytes(),
            )
        })
        return result == 1L
    }

    override fun verify(
        transactionId: String,
        tenantDigest: String,
        emailDigest: String,
        codeDigest: String,
        now: java.time.Instant,
    ): EmailOtpVerificationResult {
        val result = template.execute(RedisCallback<Long> { connection ->
            connection.scriptingCommands().eval(
                VERIFY_SCRIPT,
                ReturnType.INTEGER,
                1,
                key(transactionId),
                TENANT_FIELD,
                tenantDigest.bytes(),
                EMAIL_FIELD,
                emailDigest.bytes(),
                CODE_FIELD,
                codeDigest.bytes(),
                FAILURES_FIELD,
                MAX_ATTEMPTS_FIELD,
                EXPIRES_AT_FIELD,
                now.toEpochMilli().toString().bytes(),
            )
        }) ?: 0L
        return when (result) {
            1L -> EmailOtpVerificationResult.VERIFIED
            2L -> EmailOtpVerificationResult.EXPIRED
            3L -> EmailOtpVerificationResult.MISMATCH
            4L -> EmailOtpVerificationResult.ATTEMPTS_EXHAUSTED
            else -> EmailOtpVerificationResult.NOT_FOUND
        }
    }

    override fun cancel(transactionId: String) {
        template.delete(KEY_PREFIX + transactionId)
    }

    private fun key(transactionId: String) = (KEY_PREFIX + transactionId).bytes()
    private fun String.bytes() = toByteArray(Charsets.UTF_8)

    private companion object {
        const val KEY_PREFIX = "kudos:auth:email-otp:"
        val TENANT_FIELD = "tenant".toByteArray()
        val EMAIL_FIELD = "email".toByteArray()
        val CODE_FIELD = "code".toByteArray()
        val FAILURES_FIELD = "failures".toByteArray()
        val MAX_ATTEMPTS_FIELD = "maxAttempts".toByteArray()
        val EXPIRES_AT_FIELD = "expiresAt".toByteArray()
        val ZERO = "0".toByteArray()

        val ISSUE_SCRIPT = """
            if redis.call('EXISTS', KEYS[1]) == 1 then return 0 end
            redis.call('HSET', KEYS[1], ARGV[1], ARGV[2], ARGV[3], ARGV[4], ARGV[5], ARGV[6], ARGV[7], ARGV[8], ARGV[9], ARGV[10], ARGV[11], ARGV[12])
            redis.call('PEXPIREAT', KEYS[1], ARGV[12])
            return 1
        """.trimIndent().toByteArray()

        val VERIFY_SCRIPT = """
            if redis.call('EXISTS', KEYS[1]) == 0 then return 0 end
            local expiresAt = tonumber(redis.call('HGET', KEYS[1], ARGV[9]))
            if not expiresAt or tonumber(ARGV[10]) >= expiresAt then
                redis.call('DEL', KEYS[1])
                return 2
            end
            local failures = tonumber(redis.call('HGET', KEYS[1], ARGV[7])) or 0
            local maxAttempts = tonumber(redis.call('HGET', KEYS[1], ARGV[8])) or 1
            if failures >= maxAttempts then
                redis.call('DEL', KEYS[1])
                return 4
            end
            local matches = redis.call('HGET', KEYS[1], ARGV[1]) == ARGV[2]
                and redis.call('HGET', KEYS[1], ARGV[3]) == ARGV[4]
                and redis.call('HGET', KEYS[1], ARGV[5]) == ARGV[6]
            if not matches then
                failures = failures + 1
                if failures >= maxAttempts then
                    redis.call('DEL', KEYS[1])
                    return 4
                end
                redis.call('HSET', KEYS[1], ARGV[7], failures)
                return 3
            end
            redis.call('DEL', KEYS[1])
            return 1
        """.trimIndent().toByteArray()
    }
}
