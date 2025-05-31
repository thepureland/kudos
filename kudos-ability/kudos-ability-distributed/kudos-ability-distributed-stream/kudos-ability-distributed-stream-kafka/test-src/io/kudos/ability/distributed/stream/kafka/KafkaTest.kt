package io.kudos.ability.distributed.stream.kafka

import io.kudos.ability.distributed.stream.kafka.main.IKafkaMainService
import io.kudos.ability.distributed.stream.kafka.main.KafkaConsumerHandler
import io.kudos.ability.distributed.stream.kafka.main.KafkaMainService
import io.kudos.ability.distributed.stream.kafka.producer.KafkaProducerApplication
import io.kudos.base.net.IpKit
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.KafkaTestContainer
import io.kudos.test.container.NacosTestContainer
import io.kudos.test.container.PostgresTestContainer
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
 * kafka测试用例
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnableFeignClients
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
@Import(KafkaConsumerHandler::class, KafkaMainService::class)
@ActiveProfiles("kafka-main")
open class KafkaTest {

    @Autowired
    private lateinit var mainService: IKafkaMainService

    private val kafkaContainer = KafkaTestContainer.container

    private val EXECUTOR = Executors.newFixedThreadPool(3)

    @BeforeAll
    fun setUp() {
        val url = "jdbc:postgresql://${IpKit.getLocalIp()}:${PostgresTestContainer.PORT}/${PostgresTestContainer.DATABASE}"
        val args = arrayOf(
            "--spring.datasource.dynamic.datasource.postgres.url=$url",
            "--spring.cloud.stream.kafka.binder.brokers=${kafkaContainer.host}:${kafkaContainer.firstMappedPort}",
        )
        SpringApplication.run(KafkaProducerApplication::class.java, *args)
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
            val postgresThread = Thread { PostgresTestContainer.start(registry) }
            val nacosThread = Thread { NacosTestContainer.start(registry) }
            val kafkaThread = Thread { KafkaTestContainer.start(registry) }

            postgresThread.start()
            nacosThread.start()
            kafkaThread.start()

            postgresThread.join()
            nacosThread.join()
            kafkaThread.join()
        }
    }

}
