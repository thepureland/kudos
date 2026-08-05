package io.kudos.ability.distributed.notify.mq.support

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * [NotifyMqBindings] unit tests.
 *
 * Covers: constant values match spring-cloud-stream functional binding naming
 * conventions (`<bean>-out-0` / `<bean>-in-0`) and object initialization.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class NotifyMqBindingsTest {

    @Test
    fun constants_followStreamBindingNamingConvention() {
        assertNotNull(NotifyMqBindings)
        assertEquals("mqNotify", NotifyMqBindings.TOPIC)
        assertEquals("mqNotify", NotifyMqBindings.CONSUMER_BEAN)
        assertEquals("${NotifyMqBindings.CONSUMER_BEAN}-out-0", NotifyMqBindings.PRODUCER_BINDING)
        assertEquals("${NotifyMqBindings.CONSUMER_BEAN}-in-0", NotifyMqBindings.CONSUMER_BINDING)
    }
}
