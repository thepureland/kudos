package io.kudos.test.auth.webauthn

import io.kudos.ability.data.memdb.redis.init.RedisAutoConfiguration
import io.kudos.ability.data.memdb.redis.init.properties.RedisExtProperties
import io.kudos.ability.data.memdb.redis.init.properties.RedisProperties
import io.kudos.test.container.containers.RedisTestContainer
import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary

@SpringBootConfiguration(proxyBeanMethods = false)
@Import(RedisAutoConfiguration::class)
internal class RedisWebAuthnCeremonyStoreTestApplication {
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
