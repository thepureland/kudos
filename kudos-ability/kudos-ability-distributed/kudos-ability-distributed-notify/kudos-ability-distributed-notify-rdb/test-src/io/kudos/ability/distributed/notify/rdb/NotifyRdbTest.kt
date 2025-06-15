package io.kudos.ability.distributed.notify.rdb

import io.kudos.ability.distributed.notify.rdb.common.IRdbMsClinet
import io.kudos.ability.distributed.notify.rdb.ms.RdbMsApplication
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.base.net.IpKit
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.NacosTestContainer
import io.kudos.test.container.containers.PostgresTestContainer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import kotlin.test.Test

@EnableKudosTest
@EnableFeignClients
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfDockerAvailable
open class NotifyRdbTest {

    @Autowired
    private lateinit var rdbMsClinet: IRdbMsClinet

    @BeforeAll
    fun setUp() {
        val url = "jdbc:postgresql://${IpKit.getLocalIp()}:${PostgresTestContainer.PORT}/${PostgresTestContainer.DATABASE}"
        val args = arrayOf(
            "--spring.datasource.dynamic.datasource.postgres.url=$url",
        )
        SpringApplication.run(RdbMsApplication::class.java, *args)
    }

    /* 因 NotifyListenerItem 宣告静态变数，测试用例为同一jvm下，故无法模拟两组相同微服务测试，listener会被后盖前。 */
    @Test
    fun rdbNotifyTest() {
        val key = RandomStringKit.random(8, true, true)
        rdbMsClinet.change(key)
        var flag = true
        while (flag) {
            try {
                Thread.sleep(1000)
            } catch (e: Exception) {
                Thread.currentThread().interrupt()
            }
            val body = rdbMsClinet.key
            flag = !body!!.contains(key)
        }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        private fun registerProperties(registry: DynamicPropertyRegistry?) {
            val postgresThread = Thread { PostgresTestContainer.startIfNeeded(registry) }
            val nacosThread = Thread { NacosTestContainer.startIfNeeded(registry) }

            postgresThread.start()
            nacosThread.start()

            postgresThread.join()
            nacosThread.join()
        }
    }
}
