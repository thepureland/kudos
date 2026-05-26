package io.kudos.ability.distributed.stream.rocketmq

import io.kudos.ability.distributed.stream.rocketmq.main.IRocketMqMainService
import io.kudos.ability.distributed.stream.rocketmq.main.RocketMqConsumerHandler
import io.kudos.ability.distributed.stream.rocketmq.main.RocketMqMainService
import io.kudos.ability.distributed.stream.rocketmq.producer.RocketMqProducerApplication
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RocketMqTestContainer
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
 * RocketMQ test cases.
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnableFeignClients
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfDockerInstalled
@Import(RocketMqConsumerHandler::class, RocketMqMainService::class)
@ActiveProfiles("rocketmq-main")
open class RocketMqTest {

    @Autowired
    private lateinit var mainService: IRocketMqMainService

    private val EXECUTOR = Executors.newFixedThreadPool(3)

    @BeforeAll
    fun setUp() {
        val args = arrayOf(
            "--spring.cloud.stream.rocketmq.binder.name-server=${RocketMqTestContainer.NAMESRV_ADDR}"
        )
        SpringApplication.run(RocketMqProducerApplication::class.java, *args)
    }


    /**
     * Send/receive test.
     */
    @Test
    fun sendAndReceiveMessageTest() {
        val task = Callable<String?> { mainService.sendAndReceiveMessage() }
        val future = EXECUTOR.submit<String?>(task)
        try {
            future.get(30, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            throw RuntimeException("Wait exceeded 30 seconds; MQ receive failed", e)
        } finally {
            future.cancel(true)
        }
    }

    /**
     * Consumption exception test.
     */
    @Test
    fun streamExceptionTest() {
        val task = Callable<String?> { mainService.errorMessage() }
        val future = EXECUTOR.submit<String?>(task)
        try {
            future.get(15, TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
            println("Exception during consumption caused the result fetch to time out!")
        } finally {
            future.cancel(true)
        }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        private fun registerProperties(registry: DynamicPropertyRegistry?) {
            RocketMqTestContainer.startIfNeeded(registry)
        }
    }

}
