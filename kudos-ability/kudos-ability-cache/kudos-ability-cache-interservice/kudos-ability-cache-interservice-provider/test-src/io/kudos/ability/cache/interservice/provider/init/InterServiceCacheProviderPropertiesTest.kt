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
    }

    @Test
    fun properties_areMutable() {
        val properties = InterServiceCacheProviderProperties().apply {
            uidCacheEnabled = true
            wrapAllRequests = true
        }

        assertTrue(properties.uidCacheEnabled)
        assertTrue(properties.wrapAllRequests)
    }
}
