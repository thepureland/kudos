package io.kudos.ms.auth.provider.webauthn.ceremony

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import io.kudos.test.auth.webauthn.RedisWebAuthnCeremonyStoreTestApplication
import jakarta.annotation.Resource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Real Redis verification for cross-node serialization, TTL and atomic one-time consumption. */
@EnableKudosTest(
    classes = [RedisWebAuthnCeremonyStoreTestApplication::class],
    properties = ["kudos.ability.data.redis.default-redis=data"],
)
@EnabledIfDockerInstalled
internal class RedisWebAuthnCeremonyStoreTest {

    @Resource
    private lateinit var redisTemplates: RedisTemplates

    @Test
    fun `two instances should share and consume ceremony exactly once`() {
        val first = RedisWebAuthnCeremonyStore(redisTemplates)
        val second = RedisWebAuthnCeremonyStore(redisTemplates)
        val id = "webauthn-${UUID.randomUUID()}"
        val now = Instant.now()
        val state = WebAuthnCeremonyState(
            id = id,
            type = WebAuthnCeremonyTypeEnum.ASSERTION,
            tenantId = "tenant-1",
            userId = null,
            requestJson = "{\"challenge\":\"server-owned\"}",
            createdAt = now,
            expiresAt = now.plusSeconds(60),
        )

        assertTrue(first.create(state))
        assertFalse(second.create(state.copy(userId = "attacker")))
        assertEquals(state, second.get(id))
        assertEquals(state, first.consume(id))
        assertNull(second.consume(id))
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
