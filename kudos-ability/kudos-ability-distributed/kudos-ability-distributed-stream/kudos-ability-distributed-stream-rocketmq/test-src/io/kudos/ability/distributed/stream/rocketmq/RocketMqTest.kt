package io.kudos.ability.distributed.stream.rocketmq

import io.kudos.ability.distributed.stream.rocketmq.main.IRocketMqMainService
import io.kudos.ability.distributed.stream.rocketmq.main.RocketMqConsumerHandler
import io.kudos.ability.distributed.stream.rocketmq.main.RocketMqMainService
import io.kudos.ability.distributed.stream.rocketmq.producer.RocketMqProducerApplication
import io.kudos.base.net.IpKit
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.NacosTestContainer
import io.kudos.test.container.PostgresTestContainer
import io.kudos.test.container.RocketMqTestContainer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


/**
 * RocketMQ测试用例
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnableFeignClients
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
@Import(RocketMqConsumerHandler::class, RocketMqMainService::class)
@ActiveProfiles("rocketmq-main")
open class RocketMqTest {

    @Autowired
    private lateinit var mainService: IRocketMqMainService

    private val EXECUTOR = Executors.newFixedThreadPool(3)

    @BeforeAll
    fun setUp() {
        val url = "jdbc:postgresql://${IpKit.getLocalIp()}:${PostgresTestContainer.PORT}/${PostgresTestContainer.DATABASE}"
        val args = arrayOf(
            "--spring.datasource.dynamic.datasource.postgres.url=$url",
            "--spring.cloud.stream.rocketmq.binder.name-server=${RocketMqTestContainer.NAMESRV_ADDR}"
        )
        SpringApplication.run(RocketMqProducerApplication::class.java, *args)
    }


    /**
     * 发送与接收测试
     */
    @Test
    fun sendAndReceiveMessageTest() {
        val task = Callable<String?> { mainService.sendAndReceiveMessage() }
        val future = EXECUTOR.submit<String?>(task)
        try {
            future.get(30, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw e
        } catch (_ : TimeoutException) {
            throw RuntimeException("等待时间超过30秒，mq接收异常")
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
            future.get(15, TimeUnit.SECONDS)
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
            val postgresThread = Thread { PostgresTestContainer.start(registry) }
            val nacosThread = Thread { NacosTestContainer.start(registry) }
            val rocketMqThread = Thread { RocketMqTestContainer.start(registry) }

            postgresThread.start()
            nacosThread.start()
            rocketMqThread.start()

            postgresThread.join()
            nacosThread.join()
            rocketMqThread.join()
        }
    }

}
