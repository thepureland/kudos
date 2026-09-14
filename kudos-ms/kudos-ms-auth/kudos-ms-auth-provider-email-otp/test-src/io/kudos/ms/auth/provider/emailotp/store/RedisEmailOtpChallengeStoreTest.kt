package io.kudos.ms.auth.provider.emailotp.store

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.test.auth.emailotp.RedisEmailOtpChallengeStoreTestApplication
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import jakarta.annotation.Resource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@EnableKudosTest(
    classes = [RedisEmailOtpChallengeStoreTestApplication::class],
    properties = ["kudos.ability.data.redis.default-redis=data"],
)
@EnabledIfDockerInstalled
internal class RedisEmailOtpChallengeStoreTest {
    @Resource
    private lateinit var redisTemplates: RedisTemplates

    @Test
    fun `two instances atomically share consume and bound failures`() {
        val first = RedisEmailOtpChallengeStore(redisTemplates)
        val second = RedisEmailOtpChallengeStore(redisTemplates)
        val now = Instant.now()
        val id = "email-otp-${UUID.randomUUID()}"
        val challenge = EmailOtpChallenge(id, "tenant", "email", "code", now.plusSeconds(60), 2)

        assertTrue(first.issue(challenge))
        assertFalse(second.issue(challenge.copy(codeDigest = "attacker")))
        assertEquals(
            EmailOtpVerificationResult.MISMATCH,
            second.verify(id, "tenant", "email", "wrong", now),
        )
        assertEquals(
            EmailOtpVerificationResult.VERIFIED,
            first.verify(id, "tenant", "email", "code", now),
        )
        assertEquals(
            EmailOtpVerificationResult.NOT_FOUND,
            second.verify(id, "tenant", "email", "code", now),
        )

        val locked = "$id-locked"
        assertTrue(first.issue(challenge.copy(transactionId = locked)))
        assertEquals(
            EmailOtpVerificationResult.MISMATCH,
            first.verify(locked, "tenant", "email", "bad", now),
        )
        assertEquals(
            EmailOtpVerificationResult.ATTEMPTS_EXHAUSTED,
            second.verify(locked, "tenant", "email", "bad", now),
        )
        assertEquals(
            EmailOtpVerificationResult.NOT_FOUND,
            first.verify(locked, "tenant", "email", "code", now),
        )
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry?) {
            RedisTestContainer.startIfNeeded(registry)
            registry?.add("kudos.ability.data.redis.default-redis") { "data" }
        }
    }
}
