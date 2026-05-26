package io.kudos.ability.distributed.stream.kafka

import com.github.dockerjava.api.model.Container
import io.kudos.ability.distributed.stream.kafka.main.IKafkaMainService
import io.kudos.ability.distributed.stream.kafka.main.KafkaConsumerHandler
import io.kudos.ability.distributed.stream.kafka.main.KafkaMainService
import io.kudos.ability.distributed.stream.kafka.producer.KafkaProducerApplication
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.KafkaTestContainer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.Test


/**
 * Kafka test cases.
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnableFeignClients
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfDockerInstalled
@Import(KafkaConsumerHandler::class, KafkaMainService::class)
@ActiveProfiles("kafka-main")
open class KafkaTest {

    @Autowired
    private lateinit var mainService: IKafkaMainService

    private val executor = Executors.newFixedThreadPool(3)

    @BeforeAll
    fun setUp() {
        val args = arrayOf(
            "--spring.cloud.stream.kafka.binder.brokers=${kafkaContainer.ports.first().ip}:${kafkaContainer.ports.first().publicPort}",
        )
        SpringApplication.run(KafkaProducerApplication::class.java, *args)
    }


    /**
     * Send and receive test.
     */
    @Test
    fun sendAndReceiveMessageTest() {
        val task = Callable<String?> { mainService.sendAndReceiveMessage() }
        val future = executor.submit<String?>(task)
        try {
            future.get(5, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            throw RuntimeException("Wait exceeded 5 seconds; mq receive failed", e)
        } finally {
            future.cancel(true)
        }
    }

    /**
     * Consumer exception test.
     */
    @Test
    fun streamExceptionTest() {
        val task = Callable<String?> { mainService.errorMessage() }
        val future = executor.submit<String?>(task)
        try {
            future.get(5, TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
            println("Exception during message consumption caused a timeout while fetching the result!")
        } finally {
            future.cancel(true)
        }
    }

    companion object {
        private lateinit var kafkaContainer : Container

        @JvmStatic
        @DynamicPropertySource
        private fun registerProperties(registry: DynamicPropertyRegistry?) {
            kafkaContainer = KafkaTestContainer.startIfNeeded(registry)
        }
    }

}
