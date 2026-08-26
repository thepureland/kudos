package io.kudos.ms.auth.provider.oauth2.authorization

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.init.RedisAutoConfiguration
import io.kudos.ability.data.memdb.redis.init.properties.RedisExtProperties
import io.kudos.ability.data.memdb.redis.init.properties.RedisProperties
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import jakarta.annotation.Resource
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Real Redis verification for cross-node serialization, TTL and atomic callback consumption. */
@EnableKudosTest(
    classes = [RedisAuthorizationRequestStoreTestApplication::class],
    properties = ["kudos.ability.data.redis.default-redis=data"],
)
@EnabledIfDockerInstalled
internal class RedisExternalAuthorizationRequestStoreTest {

    @Resource
    private lateinit var redisTemplates: RedisTemplates

    @Test
    fun twoInstancesShareAndAtomicallyConsumeCompleteAuthorizationSnapshot() {
        val first = RedisExternalAuthorizationRequestStore(redisTemplates)
        val second = RedisExternalAuthorizationRequestStore(redisTemplates)
        val state = "state-${UUID.randomUUID()}"
        val request = authorizationRequest(state)

        try {
            assertTrue(first.create(request, Instant.now().plusSeconds(60)))
            assertFalse(second.create(request, Instant.now().plusSeconds(60)))
            assertEquals(request, second.get(state))
            assertEquals("verifier", second.get(state)?.getAttribute("code_verifier"))
            assertEquals(request, first.consume(state))
            assertNull(second.consume(state))
        } finally {
            first.delete(state)
        }
    }

    private fun authorizationRequest(state: String) = OAuth2AuthorizationRequest.authorizationCode()
        .authorizationUri("https://issuer.example/authorize")
        .clientId("client")
        .redirectUri("https://app.example/callback")
        .scopes(setOf("openid"))
        .state(state)
        .attributes { it["code_verifier"] = "verifier" }
        .additionalParameters { it["nonce"] = "nonce-1" }
        .build()

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry?) {
            RedisTestContainer.startIfNeeded(registry)
            registry?.add("kudos.ability.data.redis.default-redis") { "data" }
        }
    }
}

@SpringBootConfiguration(proxyBeanMethods = false)
@Import(RedisAutoConfiguration::class)
private class RedisAuthorizationRequestStoreTestApplication {
    @Bean
    @Primary
    fun testRedisProperties(): RedisProperties {
        val container = requireNotNull(RedisTestContainer.getRunningContainer())
        val port = requireNotNull(container.ports.firstOrNull())
        return RedisProperties().apply {
            defaultRedis = "data"
            redisMap["data"] = RedisExtProperties().apply {
                host = requireNotNull(port.ip)
                this.port = requireNotNull(port.publicPort)
            }
        }
    }
}
