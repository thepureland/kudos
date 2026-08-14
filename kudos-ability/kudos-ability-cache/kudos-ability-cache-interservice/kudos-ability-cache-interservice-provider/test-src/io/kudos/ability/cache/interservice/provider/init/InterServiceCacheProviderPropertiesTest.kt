package io.kudos.ability.cache.interservice.provider.init

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for InterServiceCacheProviderProperties
 *
 * Covers default values and property mutation.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class InterServiceCacheProviderPropertiesTest {

    @Test
    fun defaults_areDisabled() {
        val properties = InterServiceCacheProviderProperties()

        assertFalse(properties.uidCacheEnabled)
        assertFalse(properties.wrapAllRequests)
        // 默认关闭是刻意的：开启后若调用方没带内部标记，整个 provider 端会静默失效，
        // 而标记来自另一个模块，运行时并不保证存在。详见该属性的 KDoc。
        assertFalse(properties.requireFeignMarker)
    }

    @Test
    fun properties_areMutable() {
        val properties = InterServiceCacheProviderProperties().apply {
            uidCacheEnabled = true
            wrapAllRequests = true
            requireFeignMarker = true
        }

        assertTrue(properties.uidCacheEnabled)
        assertTrue(properties.wrapAllRequests)
        assertTrue(properties.requireFeignMarker)
    }
}
