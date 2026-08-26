package io.kudos.ms.auth.core.authentication.session

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.core.authentication.session.store.IAuthenticationSessionStore
import io.kudos.ms.auth.core.authentication.session.store.RedisAuthenticationSessionStore
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

/** Real Redis verification for cross-instance authentication-session create/CAS and serialization. */
@EnabledIfDockerInstalled
internal class RedisAuthenticationSessionStoreTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var redisTemplates: RedisTemplates

    @Resource
    private lateinit var configuredStore: IAuthenticationSessionStore

    @Test
    fun twoInstancesShareAtomicCreateAndVersionedUpdates() {
        assertIs<RedisAuthenticationSessionStore>(configuredStore)
        val first = RedisAuthenticationSessionStore(redisTemplates)
        val second = RedisAuthenticationSessionStore(redisTemplates)
        val now = Instant.now()
        val session = AuthenticationSession(
            id = UUID.randomUUID().toString(),
            tenantId = "t-1",
            userId = "u-1",
            authTime = now,
            amr = setOf("password"),
            acr = "urn:kudos:acr:password",
            createdAt = now,
            lastSeenAt = now,
            idleExpiresAt = now.plusSeconds(30),
            absoluteExpiresAt = now.plusSeconds(60),
        )

        assertTrue(first.create(session))
        assertFalse(second.create(session))
        assertEquals(session, second.get(session.id))
        assertEquals(listOf(session), second.findByUser("t-1", "u-1"))
        assertTrue(second.findByUser("t-1", "u-other").isEmpty())

        val updated = assertNotNull(first.save(session.copy(revokeReason = "first"), 0))
        assertEquals(1, updated.version)
        assertNull(second.save(session.copy(revokeReason = "stale"), 0))
        assertEquals("first", second.get(session.id)?.revokeReason)
    }
}
