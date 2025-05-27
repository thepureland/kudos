package io.kudos.ability.distributed.notify.rdb

import io.kudos.ability.distributed.notify.rdb.common.IRdbMsClinet
import io.kudos.ability.distributed.notify.rdb.ms.RdbMsApplication
import io.kudos.test.common.init.EnableKudosTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.soul.base.lang.string.RandomStringTool
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers

@EnableKudosTest
@EnableFeignClients
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
class NotifyRdbTest {
    private var msApplication: ConfigurableApplicationContext? = null

    @Autowired
    private val rdbMsClinet: IRdbMsClinet? = null

    @BeforeAll
    @Throws(InterruptedException::class)
    fun setUp() {
        val url = ""
        //        String url = "jdbc:postgresql://%s:%s/%s".formatted(IpTool.getLocalIp(), TestContainerPostgres.PORT, TestContainerPostgres.DATABASE);
        val args: Array<String?> = arrayOf<String>(
            "--spring.datasource.dynamic.datasource.postgres.url=" + url
        )
        msApplication = SpringApplication.run(RdbMsApplication::class.java, *args)
    }

    @AfterAll
    fun tearDown() {
        if (msApplication != null) msApplication!!.close()
    }

    /* 因 NotifyListenerItem 宣告静态变数，测试用例为同一jvm下，故无法模拟两组相同微服务测试，listener会被后盖前。 */
    @Test
    fun rdbNotifyTest() {
        val key = RandomStringTool.random(8, true, true)
        rdbMsClinet!!.change(key)
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
        @DynamicPropertySource
        @Throws(InterruptedException::class)
        private fun registerProperties(registry: DynamicPropertyRegistry?) {
//        PostgresTestContainer.start(registry);
//        NacosTestContainer.start(registry);
        }
    }
}
