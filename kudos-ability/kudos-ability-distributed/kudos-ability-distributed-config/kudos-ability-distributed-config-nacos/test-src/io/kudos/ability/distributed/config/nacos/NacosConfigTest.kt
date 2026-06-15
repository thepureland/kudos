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
import kotlin.test.assertSame
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
        // The test container binds the fixed host port 28848. If some other process (e.g. a
        // parallel build of a sibling project) already serves Nacos on the configured address,
        // starting our own container would fail with "port is already allocated" — in that case
        // simply reuse the running instance; the test only touches its own unique dataIds.
        if (!isNacosServing(serverAddr!!)) {
            NacosTestContainer.startIfNeeded(null)
        }

        val properties = Properties()
        properties["serverAddr"] = serverAddr
        configService = NacosFactory.createConfigService(properties)
    }

    /** Probe whether a Nacos server is already answering HTTP on the given `host:port` address. */
    private fun isNacosServing(addr: String): Boolean = try {
        val connection = java.net.URI("http://$addr/nacos/v1/console/health/readiness")
            .toURL().openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 2000
        connection.readTimeout = 2000
        connection.requestMethod = "GET"
        val alive = connection.responseCode in 200..499
        connection.disconnect()
        alive
    } catch (_: Exception) {
        false
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
        // Unique content per run: when the Nacos instance is reused across runs, republishing
        // identical content yields no MD5 change and therefore no listener notification.
        val content = "testContent1-${System.currentTimeMillis()}"

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

    @Test
    fun testRemoveListener() {
        val dataId = "testDataId2"
        val group = "testGroup2"

        // Build via PropertiesBuilder constructor (covers the second constructor against a real server)
        val listenerHolder = NacosConfigServiceListener(
            NacosConfigServiceListener.PropertiesBuilder()
                .put(NacosConfigServiceListener.PRO_SERVER_ADDR_KEY, serverAddr)
        )

        var receiveCount = 0
        val listener = object : AbstractConfigChangeListener() {
            override fun onConfigChanged(configInfo: String?) {
                receiveCount++
            }
        }
        listenerHolder.addListener(dataId, group, listener)

        // Publish once and wait until the listener fires.
        // Unique content per run: if the container is reused, republishing identical content
        // produces no MD5 change and therefore no notification.
        val stamp = System.currentTimeMillis()
        assertTrue(configService.publishConfig(dataId, group, "v1-$stamp"))
        var count = 0
        while (receiveCount == 0 && count < 10) {
            Thread.sleep(1000L)
            count++
        }
        assertEquals(1, receiveCount, "listener should have received exactly one change")

        // Remove the listener, publish again: no further callback expected
        listenerHolder.removeListener(dataId, group, listener)
        assertTrue(configService.publishConfig(dataId, group, "v2-$stamp"))
        Thread.sleep(3000L)
        assertEquals(1, receiveCount, "after removeListener no further change events may arrive")
    }

    @Test
    fun testConfigServiceCacheReuse() {
        // Same (serverAddr, namespace) pair must reuse the same underlying ConfigService instance
        val l1 = NacosConfigServiceListener(serverAddr)
        val l2 = NacosConfigServiceListener(
            NacosConfigServiceListener.PropertiesBuilder()
                .put(NacosConfigServiceListener.PRO_SERVER_ADDR_KEY, serverAddr)
        )

        val field = NacosConfigServiceListener::class.java.getDeclaredField("configService")
            .apply { isAccessible = true }
        assertSame(
            field.get(l1), field.get(l2),
            "the same (serverAddr, namespace) must map to one cached ConfigService",
        )
    }

}
