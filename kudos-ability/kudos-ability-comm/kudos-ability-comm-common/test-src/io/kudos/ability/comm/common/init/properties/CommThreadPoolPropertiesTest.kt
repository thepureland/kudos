package io.kudos.ability.comm.common.init.properties

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for CommThreadPoolProperties
 *
 * 覆蓋：全部屬性的默認值校驗、自定義值賦值與讀回（含邊界值 0、負數、空字符串、Unicode 前綴）。
 *
 * @author K
 * @since 1.0.0
 */
internal class CommThreadPoolPropertiesTest {

    @Test
    fun defaults() {
        val properties = CommThreadPoolProperties()

        assertEquals("comm-pool", properties.threadNamePrefix)
        assertEquals(3, properties.corePoolSize)
        assertEquals(10, properties.maxPoolSize)
        assertEquals(900, properties.keepAliveSeconds)
        assertEquals(100, properties.queueCapacity)
    }

    @Test
    fun acceptsConfiguredValues() {
        val properties = CommThreadPoolProperties()

        properties.threadNamePrefix = "sms-pool"
        properties.corePoolSize = 1
        properties.maxPoolSize = 64
        properties.keepAliveSeconds = 60
        properties.queueCapacity = 2048

        assertEquals("sms-pool", properties.threadNamePrefix)
        assertEquals(1, properties.corePoolSize)
        assertEquals(64, properties.maxPoolSize)
        assertEquals(60, properties.keepAliveSeconds)
        assertEquals(2048, properties.queueCapacity)
    }

    @Test
    fun acceptsBoundaryAndUnusualValues() {
        // POJO 配置類不做校驗，邊界/異常值由使用方（線程池構建處）負責約束
        val properties = CommThreadPoolProperties()

        properties.threadNamePrefix = ""
        assertEquals("", properties.threadNamePrefix)

        properties.threadNamePrefix = "通信-線程池-α"
        assertEquals("通信-線程池-α", properties.threadNamePrefix)

        properties.corePoolSize = 0
        properties.maxPoolSize = 0
        properties.keepAliveSeconds = 0
        properties.queueCapacity = 0
        assertEquals(0, properties.corePoolSize)
        assertEquals(0, properties.maxPoolSize)
        assertEquals(0, properties.keepAliveSeconds)
        assertEquals(0, properties.queueCapacity)

        properties.corePoolSize = -1
        properties.maxPoolSize = Int.MAX_VALUE
        properties.keepAliveSeconds = Int.MIN_VALUE
        properties.queueCapacity = -100
        assertEquals(-1, properties.corePoolSize)
        assertEquals(Int.MAX_VALUE, properties.maxPoolSize)
        assertEquals(Int.MIN_VALUE, properties.keepAliveSeconds)
        assertEquals(-100, properties.queueCapacity)
    }

}
