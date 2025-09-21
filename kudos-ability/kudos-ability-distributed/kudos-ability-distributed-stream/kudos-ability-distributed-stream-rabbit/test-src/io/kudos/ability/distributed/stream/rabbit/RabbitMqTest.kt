package io.kudos.ability.distributed.stream.rabbit

import io.kudos.ability.distributed.stream.rabbit.main.IRabbitMqMainService
import io.kudos.ability.distributed.stream.rabbit.main.RabbitMqConsumerHandler
import io.kudos.ability.distributed.stream.rabbit.main.RabbitMqMainService
import io.kudos.ability.distributed.stream.rabbit.producer.RabbitMqProducerApplication
import io.kudos.base.net.IpKit
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.RabbitMqTestContainer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.Test


/**
 * RabbitMq测试用例
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnableFeignClients
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfDockerAvailable
@Import(RabbitMqConsumerHandler::class, RabbitMqMainService::class)
@ActiveProfiles("rabbit-main")
open class RabbitMqTest {

    @Autowired
    private lateinit var mainService: IRabbitMqMainService

    private val EXECUTOR = Executors.newFixedThreadPool(3)

    @BeforeAll
    fun setUp() {
        val args = arrayOf(
            "--spring.rabbitmq.host=${IpKit.getLocalIp()}",
            "--spring.rabbitmq.username=guest",
            "--spring.rabbitmq.password=guest",
            "--spring.rabbitmq.port=${RabbitMqTestContainer.PORT}",
            "--spring.rabbitmq.virtual-host=/",
            "--spring.rabbitmq.addresses=${IpKit.getLocalIp()}:${RabbitMqTestContainer.PORT}"
            )
        SpringApplication.run(RabbitMqProducerApplication::class.java, *args)
    }


    /**
     * 发送与接收测试
     */
    @Test
    fun sendAndReceiveMessageTest() {
        val task = Callable<String?> { mainService.sendAndReceiveMessage() }
        val future = EXECUTOR.submit<String?>(task)
        try {
            future.get(5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw e
        } catch (_ : TimeoutException) {
            throw RuntimeException("等待时间超过5秒，mq接收异常")
        } finally {
            future.cancel(true)
        }
    }

    /**
     * 消费信息异常测试
     */
    @Test
    fun streamExceptionTest() {
        val task = Callable<String?> { mainService.errorMessage() }
        val future = EXECUTOR.submit<String?>(task)
        try {
            future.get(5, TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
            println("消费信息时异常，造成获取结果超时！")
        } finally {
            future.cancel(true)
        }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        private fun registerProperties(registry: DynamicPropertyRegistry?) {
            RabbitMqTestContainer.startIfNeeded(registry)
        }
    }

}
