package io.kudos.ability.distributed.discovery.nacos

import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.NacosTestContainer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * Nacos service discovery test.
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
@ActiveProfiles("client")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class NacosServiceDiscoveryTest @Autowired constructor(
    private val discoveryClient: DiscoveryClient
) {

    @BeforeAll
    fun setup() {
        NacosTestContainer.startIfNeeded(null)
        println("########## starting mock microservice...")
        SpringApplication.run(MockMsApplication::class.java)
        println("########## mock microservice started successfully.")
    }

    @Test
    fun test() {
        val services = discoveryClient.getInstances("discovery")
        assertFalse(services.isEmpty())
    }

}
