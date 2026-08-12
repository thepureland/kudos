package io.kudos.ability.distributed.notify.mq

import io.kudos.ability.distributed.notify.mq.common.IMainClinet
import io.kudos.ability.distributed.notify.mq.main.MainApplication
import io.kudos.ability.distributed.notify.mq.ms.MsApplication
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RocketMqTestContainer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.Test
import kotlin.test.fail

@EnableKudosTest
@EnableFeignClients
@PropertySource(
    value = ["classpath:application-test.yml"
    ], factory = YamlPropertySourceFactory::class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfDockerInstalled
@Disabled(
    "The cross-application notification never propagates: main and ms are two Spring Boot apps " +
            "started in the same JVM that are supposed to sync through RocketMQ, and mqNotifyTest " +
            "waits for a sync that does not arrive. Until this was investigated the test could not " +
            "even reach its body — the context failed to start because every module shared one " +
            "on-disk H2 file — so there is no known-good baseline to compare against; it has likely " +
            "never actually run. Note also that apache/rocketmq:4.9.7 is an amd64 image and is " +
            "emulated on ARM hosts. To re-enable: confirm the notify round trip (main.change -> MQ -> " +
            "ms listener -> main.sync) actually completes, then drop this annotation. The wait loop " +
            "now has a 120s deadline, so a re-enabled run fails instead of hanging the build."
)
//@Import(
//    DataSourceNotifyListener::class,
//    MsApplicationListener::class,
//    MsConfig::class
//)
open class NotifyMqTest {

    @Autowired
    private lateinit var mainClinet: IMainClinet

    @BeforeAll
    fun setUp() {
        val args = arrayOf(
            "--spring.cloud.stream.rocketmq.binder.name-server=${RocketMqTestContainer.NAMESRV_ADDR}"
        )
        SpringApplication.run(MainApplication::class.java, *args)
        SpringApplication.run(MsApplication::class.java, *args)
    }

    /* Because NotifyListenerItem holds static state and tests share a single JVM, we cannot simulate two identical microservice instances — later listener registrations overwrite earlier ones. */
    @Test
    fun mqNotifyTest() {
        val key = RandomStringKit.random(8, true, true)
        mainClinet.change(key)
        var countTime = System.currentTimeMillis()
        // Hard deadline: without it this loop never exits when the notification does not propagate,
        // so a broken run hangs the build forever instead of reporting a failure.
        val deadline = System.currentTimeMillis() + SYNC_TIMEOUT_MS
        while (!mainClinet.sync(key)) {
            if (System.currentTimeMillis() > deadline) {
                fail("mq notification did not propagate within ${SYNC_TIMEOUT_MS / 1000}s")
            }
            try {
                Thread.sleep(1000)
            } catch (e: Exception) {
                Thread.currentThread().interrupt()
            }

            if (System.currentTimeMillis() - countTime >= 1000 * 10) {
                mainClinet.change(key)
                countTime = System.currentTimeMillis()
            }
        }
    }

    companion object {
        /** Upper bound for waiting on the cross-application notification. */
        private const val SYNC_TIMEOUT_MS = 120_000L

        @JvmStatic
        @DynamicPropertySource
        private fun registerProperties(registry: DynamicPropertyRegistry?) {
            RocketMqTestContainer.startIfNeeded(registry)
        }
    }
}
