package io.kudos.ability.distributed.notify.common.init.properties

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [NotifyCommonProperties].
 *
 * Covers documented defaults (failOnMissingProducer=false, listenerNamespace=null,
 * fallbackToDefaultNamespace=false) and mutability of every property, including
 * resetting listenerNamespace back to null and Unicode / blank values.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class NotifyCommonPropertiesTest {

    @Test
    fun defaults_matchDocumentedValues() {
        val properties = NotifyCommonProperties()

        assertFalse(properties.failOnMissingProducer)
        assertNull(properties.listenerNamespace)
        assertFalse(properties.fallbackToDefaultNamespace)
    }

    @Test
    fun setters_mutateAllProperties() {
        val properties = NotifyCommonProperties().apply {
            failOnMissingProducer = true
            listenerNamespace = "我的命名空间-ns_1"
            fallbackToDefaultNamespace = true
        }

        assertTrue(properties.failOnMissingProducer)
        assertEquals("我的命名空间-ns_1", properties.listenerNamespace)
        assertTrue(properties.fallbackToDefaultNamespace)
    }

    @Test
    fun listenerNamespace_acceptsBlankAndCanBeResetToNull() {
        val properties = NotifyCommonProperties()

        properties.listenerNamespace = "   "
        assertEquals("   ", properties.listenerNamespace)

        properties.listenerNamespace = null
        assertNull(properties.listenerNamespace)
    }

}
