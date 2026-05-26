package io.kudos.ability.cache.interservice

import io.kudos.ability.cache.interservice.client.IMockProxy
import io.kudos.ability.cache.interservice.provider.MockMsApplication
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.SpringApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test

/**
 * Inter-service cache test cases.
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnableFeignClients
@ActiveProfiles("client")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class InterServiceCacheTest {

    @Resource
    private lateinit var proxy: IMockProxy

    @BeforeAll
    fun setup() {
        println("########## Starting the mock microservice...")
        SpringApplication.run(MockMsApplication::class.java)
        println("########## Mock microservice started successfully.")
    }

    @Test
    fun same() {
        val obj1 = proxy.same()
        val obj2 = proxy.same()
        assert(obj1 === obj2)
    }

    @Test
    fun different1() {
        val obj1 = proxy.different1()
        val obj2 = proxy.different1()
        assert(obj1 !== obj2)
    }

    @Test
    fun different2() {
        val obj1 = proxy.different2()
        val obj2 = proxy.different2()
        assert(obj1 !== obj2)
    }

}