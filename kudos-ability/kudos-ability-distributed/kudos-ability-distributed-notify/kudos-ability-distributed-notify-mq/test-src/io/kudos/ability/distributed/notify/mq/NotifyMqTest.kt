package io.kudos.ability.distributed.notify.mq

import io.kudos.ability.distributed.notify.mq.common.IMainClinet
import io.kudos.ability.distributed.notify.mq.main.MainApplication
import io.kudos.ability.distributed.notify.mq.ms.MsApplication
import io.kudos.base.net.IpKit
import io.kudos.context.spring.YamlPropertySourceFactory
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.NacosTestContainer
import io.kudos.test.container.PostgresTestContainer
import io.kudos.test.container.RocketMqTestContainer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.soul.base.lang.string.RandomStringTool
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers

@EnableKudosTest
@EnableFeignClients
@PropertySource(
    value = ["classpath:application-test.yml"
    ], factory = YamlPropertySourceFactory::class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
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
        val key = RandomStringTool.random(8, true, true)
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
