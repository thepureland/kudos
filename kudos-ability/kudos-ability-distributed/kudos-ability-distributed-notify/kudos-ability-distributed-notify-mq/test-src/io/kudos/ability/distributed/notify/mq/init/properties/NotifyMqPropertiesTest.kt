package io.kudos.ability.distributed.notify.mq.init.properties

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [NotifyMqProperties] unit tests.
 *
 * Covers: default values (both switches false, preserving historical lenient behavior)
 * and mutability of each property.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class NotifyMqPropertiesTest {

    @Test
    fun defaults_areAllFalse() {
        val props = NotifyMqProperties()
        assertFalse(props.failOnMissingProducerBinding)
        assertFalse(props.rethrowConsumerException)
    }

    @Test
    fun properties_areMutable() {
        val props = NotifyMqProperties().apply {
            failOnMissingProducerBinding = true
            rethrowConsumerException = true
        }
        assertTrue(props.failOnMissingProducerBinding)
        assertTrue(props.rethrowConsumerException)
    }
}
