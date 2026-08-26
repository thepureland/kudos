package io.kudos.ms.auth.core.authentication.mfa

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ms.auth.core.authentication.mfa.store.ITotpEnrollmentStore
import io.kudos.ms.auth.core.authentication.mfa.store.RedisTotpEnrollmentStore
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@EnabledIfDockerInstalled
internal class RedisTotpEnrollmentStoreTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var redisTemplates: RedisTemplates

    @Resource
    private lateinit var configuredStore: ITotpEnrollmentStore

    @Test
    fun twoInstancesShareCasAndOnlyOneCanConsume() {
        assertIs<RedisTotpEnrollmentStore>(configuredStore)
        val first = RedisTotpEnrollmentStore(redisTemplates)
        val second = RedisTotpEnrollmentStore(redisTemplates)
        val now = Instant.now()
        val enrollment = TotpEnrollment(
            id = UUID.randomUUID().toString(),
            tenantId = "t-1",
            userId = "u-1",
            encryptedSecret = "encrypted-secret",
            createdAt = now,
            expiresAt = now.plusSeconds(60),
        )

        assertTrue(first.create(enrollment))
        assertFalse(second.create(enrollment))
        assertEquals(enrollment, second.get(enrollment.id))

        val updated = assertNotNull(first.save(enrollment.copy(failedAttempts = 1), 0))
        assertEquals(1, updated.version)
        assertNull(second.save(enrollment.copy(failedAttempts = 2), 0))
        assertNull(second.consume(enrollment.id, 0))
        assertEquals(updated, first.consume(enrollment.id, 1))
        assertNull(second.consume(enrollment.id, 1))
    }
}
