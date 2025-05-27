package io.kudos.ability.distributed.notify.mq

import io.kudos.ability.distributed.notify.mq.common.IMainClinet
import io.kudos.ability.distributed.notify.mq.main.MainApplication
import io.kudos.ability.distributed.notify.mq.ms.MsApplication
import io.kudos.context.spring.YamlPropertySourceFactory
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.NacosTestContainer
import io.kudos.test.container.PostgresTestContainer
import io.kudos.test.container.RocketMqTestContainer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.soul.base.lang.string.RandomStringTool
import org.soul.base.net.IpTool
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.ComponentScan
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
@ComponentScan(
    basePackages = ["org.soul.ability.distributed.notify.mq.common"
    ]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
class NotifyMqTest {
    private var mainApplication: ConfigurableApplicationContext? = null
    private var msApplication: ConfigurableApplicationContext? = null

    @Autowired
    private val mainClinet: IMainClinet? = null

    @BeforeAll
    @Throws(InterruptedException::class)
    fun setUp() {
        val url: String = "jdbc:postgresql://%s:%s/%s".formatted(
            IpTool.getLocalIp(),
            PostgresTestContainer.PORT,
            PostgresTestContainer.DATABASE
        )
        val args: Array<String?> = arrayOf<String>(
            "--spring.datasource.dynamic.datasource.postgres.url=" + url,
            "--soul.ability.distributed.stream.mq-config.rock-mq-name-server=" + RocketMqTestContainer.getHost(),
        )
        mainApplication = SpringApplication.run(MainApplication::class.java, *args)
        msApplication = SpringApplication.run(MsApplication::class.java, *args)
    }

    @AfterAll
    fun tearDown() {
        if (mainApplication != null) {
            mainApplication!!.close()
        }
        if (msApplication != null) {
            msApplication!!.close()
        }
    }

    /* 因 NotifyListenerItem 宣告静态变数，测试用例为同一jvm下，故无法模拟两组相同微服务测试，listener会被后盖前。 */
    @Test
    fun mqNotifyTest() {
        val key = RandomStringTool.random(8, true, true)
        mainClinet!!.change(key)
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
        @DynamicPropertySource
        @Throws(InterruptedException::class)
        private fun registerProperties(registry: DynamicPropertyRegistry?) {
            PostgresTestContainer.start(registry)
            NacosTestContainer.start(registry)
            RocketMqTestContainer.start(registry)
        }
    }
}
