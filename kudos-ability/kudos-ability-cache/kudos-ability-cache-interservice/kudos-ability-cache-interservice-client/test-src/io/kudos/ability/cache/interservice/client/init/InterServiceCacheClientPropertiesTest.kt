package io.kudos.ability.cache.interservice.client.init

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [InterServiceCacheClientProperties]: default values and mutability of every property.
 *
 * @author K
 * @since 1.0.0
 */
internal class InterServiceCacheClientPropertiesTest {

    @Test
    fun defaults_matchDocumentedValues() {
        val properties = InterServiceCacheClientProperties()
        assertEquals(600, properties.ttlSeconds, "default TTL must be 10 minutes")
    }

    @Test
    fun setters_overrideValues() {
        val properties = InterServiceCacheClientProperties().apply { ttlSeconds = 1 }
        assertEquals(1, properties.ttlSeconds)
    }

    // decoderEnabled 曾在这里，但它是死字段：真正生效的是 @ConditionalOnProperty 直接读环境里的
    // decoder-enabled，而条件在任何 bean 存在之前就已求值，字段不可能左右它。断言字段默认值只会
    // 固化"看起来能用"的假象，因此随字段一并删除；开关本身仍可通过 yml 配置。
}
