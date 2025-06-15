package io.kudos.ability.distributed.notify.mq

import io.kudos.ability.distributed.notify.mq.common.IMainClinet
import io.kudos.ability.distributed.notify.mq.main.MainApplication
import io.kudos.ability.distributed.notify.mq.ms.MsApplication
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.base.net.IpKit
import io.kudos.context.spring.YamlPropertySourceFactory
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.NacosTestContainer
import io.kudos.test.container.containers.PostgresTestContainer
import io.kudos.test.container.containers.RocketMqTestContainer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import kotlin.test.Test

@EnableKudosTest
@EnableFeignClients
@PropertySource(
    value = ["classpath:application-test.yml"
    ], factory = YamlPropertySourceFactory::class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfDockerAvailable
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
        val url = "jdbc:postgresql://${IpKit.getLocalIp()}:${PostgresTestContainer.PORT}/${PostgresTestContainer.DATABASE}"
        val args = arrayOf(
            "--spring.datasource.dynamic.datasource.postgres.url=$url",
            "--spring.cloud.stream.rocketmq.binder.name-server=${RocketMqTestContainer.NAMESRV_ADDR}"
        )
        SpringApplication.run(MainApplication::class.java, *args)
        SpringApplication.run(MsApplication::class.java, *args)
    }

    /* 因 NotifyListenerItem 宣告静态变数，测试用例为同一jvm下，故无法模拟两组相同微服务测试，listener会被后盖前。 */
    @Test
    fun mqNotifyTest() {
        val key = RandomStringKit.random(8, true, true)
        mainClinet.change(key)
        var countTime = System.currentTimeMillis()
        while (!mainClinet.sync(key)) {
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
        @JvmStatic
        @DynamicPropertySource
        private fun registerProperties(registry: DynamicPropertyRegistry?) {
            val postgresThread = Thread { PostgresTestContainer.startIfNeeded(registry) }
            val nacosThread = Thread { NacosTestContainer.startIfNeeded(registry) }
            val rocketMqThread = Thread { RocketMqTestContainer.startIfNeeded(registry) }

            postgresThread.start()
            nacosThread.start()
            rocketMqThread.start()

            postgresThread.join()
            nacosThread.join()
            rocketMqThread.join()
        }
    }
}
