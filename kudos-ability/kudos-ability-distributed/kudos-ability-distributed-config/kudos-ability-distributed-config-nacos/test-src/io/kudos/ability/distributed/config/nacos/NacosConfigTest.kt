package io.kudos.ability.distributed.config.nacos

import com.alibaba.nacos.api.NacosFactory
import com.alibaba.nacos.api.config.ConfigService
import io.kudos.ability.distributed.config.nacos.listener.AbstractConfigChangeListener
import io.kudos.ability.distributed.config.nacos.listener.NacosConfigServiceListener
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.NacosTestContainer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Value
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test cases for Nacos as a configuration center.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
open class NacosConfigTest {

    @Value($$"${spring.cloud.nacos.config.server-addr}")
    private val serverAddr: String? = null

    private lateinit var configService: ConfigService

    @BeforeAll
    fun setup() {
        NacosTestContainer.startIfNeeded(null)

        val properties = Properties()
        properties["serverAddr"] = serverAddr
        configService = NacosFactory.createConfigService(properties)
    }

    @Test
    fun testPublishAndRead() {
        val dataId = "testDataId"
        val group = "testGroup"
        val content = "testContent"

        // Publish config
        assertTrue(configService.publishConfig(dataId, group, content))

        // Read config; since client refresh takes time, poll with a delay
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

        // Verify
        assertEquals(content, remoteConfig)
    }

    @Test
    fun testListener() {
        val dataId = "testDataId1"
        val group = "testGroup1"
        val content = "testContent1"

        // Subscribe
        var receiveConfig: String? = null
        NacosConfigServiceListener(serverAddr).addListener(dataId, group, object : AbstractConfigChangeListener() {
            override fun onConfigChanged(configInfo: String?) {
                receiveConfig = configInfo
            }
        })
        assertNull(receiveConfig)

        // Publish config
        assertTrue(configService.publishConfig(dataId, group, content))

        // Read config; since client refresh takes time, poll with a delay
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

        // Verify
        assertEquals(content, remoteConfig)
    }

}
