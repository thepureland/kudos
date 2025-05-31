package io.kudos.ability.distributed.client.feign

import io.kudos.ability.distributed.client.feign.client.IFeignClient
import io.kudos.ability.distributed.client.feign.ms.MockMsApplication
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.NacosTestContainer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.function.Executable
import org.soul.base.bean.Pair
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * openFeign调用测试
 *
 * @author will
 * @since 5.1.1
 */
@EnableKudosTest
@EnableFeignClients
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("client")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class OpenFeignTest {

    @Autowired
    private lateinit var feignClient: IFeignClient

    @BeforeAll
    fun setup() {
        NacosTestContainer.start(null)
        println("########## 启动模拟的微服务...")
        SpringApplication.run(MockMsApplication::class.java)
        println("########## 启动模拟的微服务成功.")
    }

    @Test
    fun test() {
        // 测试get方式的请求
        Assertions.assertTrue(feignClient.get())

        // 测试post方式的请求
        val result = feignClient.post(Pair<Int?, String?>(1, "name"))
        Assertions.assertTrue(result!!.getValue()!!)

        // 测试异常抛回
        Assertions.assertThrows(RuntimeException::class.java, Executable { feignClient.exception() })
    }

}