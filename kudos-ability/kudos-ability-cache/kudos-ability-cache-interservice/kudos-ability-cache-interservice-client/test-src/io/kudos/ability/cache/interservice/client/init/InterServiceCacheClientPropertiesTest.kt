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
        assertTrue(properties.decoderEnabled, "the decoder decoration chain must be enabled by default")
    }

    @Test
    fun setters_overrideValues() {
        val properties = InterServiceCacheClientProperties().apply {
            ttlSeconds = 1
            decoderEnabled = false
        }
        assertEquals(1, properties.ttlSeconds)
        assertFalse(properties.decoderEnabled)
    }
}
