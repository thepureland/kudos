package io.kudos.ability.distributed.config.nacos

import com.alibaba.nacos.api.NacosFactory
import com.alibaba.nacos.api.config.ConfigService
import io.kudos.ability.distributed.config.nacos.listener.AbstractConfigChangeListener
import io.kudos.ability.distributed.config.nacos.listener.NacosConfigServiceListener
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.NacosTestContainer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Value
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * nacos作为配置中心的测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerAvailable
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
open class NacosConfigTest {
    
    @Value("\${spring.cloud.nacos.config.server-addr}")
    private val serverAddr: String? = null

    private lateinit var configService: ConfigService

    @BeforeAll
    fun setup() {
        NacosTestContainer.startIfNeeded(null)

        val properties = Properties()
        properties.put("serverAddr", serverAddr)
        configService = NacosFactory.createConfigService(properties)
    }

    @Test
    fun testPublishAndRead() {
        val dataId = "testDataId"
        val group = "testGroup"
        val content = "testContent"

        // 发布配置
        assert(configService.publishConfig(dataId, group, content))

        // 读取配置，因为客户端刷新需要时间，这里循环延迟读取
        var remoteConfig: String?
        var count = 0
        while (true) {
            Thread.sleep(1000L)
            remoteConfig = configService.getConfig(dataId, group, 5000)
            count++
            if (remoteConfig != null || count == 10) {
                break
            }
        }

        // 校验
        assertEquals(content, remoteConfig)
    }

    @Test
    fun testListener() {
        val dataId = "testDataId1"
        val group = "testGroup1"
        val content = "testContent1"

        // 监听
        var receiveConfig: String? = null
        NacosConfigServiceListener(serverAddr).addListener(dataId, group, object : AbstractConfigChangeListener() {
            override fun receiveConfigInfo(configInfo: String?) {
                receiveConfig = configInfo
            }
        })
        assertNull(receiveConfig)

        // 发布配置
        assert(configService.publishConfig(dataId, group, content))

        // 读取配置，因为客户端刷新需要时间，这里循环延迟读取
        var remoteConfig: String?
        var count = 0
        while (true) {
            Thread.sleep(1000L)
            remoteConfig = receiveConfig
            count++
            if (remoteConfig != null || count == 10) {
                break
            }
        }

        // 校验
        assertEquals(content, remoteConfig)
    }

}
