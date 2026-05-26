package io.kudos.ability.distributed.client.feign

import io.kudos.ability.distributed.client.feign.client.IFeignClient
import io.kudos.ability.distributed.client.feign.ms.MockMsApplication
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.NacosTestContainer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * OpenFeign invocation tests.
 *
 * @author will
 * @since 5.1.1
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest
@EnableFeignClients
@EnabledIfDockerInstalled
@ActiveProfiles("client")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class OpenFeignTest {

    @Autowired
    private lateinit var feignClient: IFeignClient

    @BeforeAll
    fun setup() {
        NacosTestContainer.startIfNeeded(null)
        println("########## Starting the mock microservice...")
        SpringApplication.run(MockMsApplication::class.java)
        println("########## Mock microservice started.")
    }

    @Test
    fun test() {
        // Test GET request
        assertTrue(feignClient.get())

        // Test POST request
        val result = feignClient.post(PostParam(1, "name"))
        assertEquals(true, result.success)

        // Test exception propagation
        assertFailsWith<RuntimeException> { feignClient.exception() }
    }

}
