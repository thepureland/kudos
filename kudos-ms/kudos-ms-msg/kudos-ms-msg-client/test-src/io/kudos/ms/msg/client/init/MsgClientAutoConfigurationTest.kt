package io.kudos.ms.msg.client.init

import io.kudos.context.init.IComponentInitializer
import kotlin.test.Test
import kotlin.test.assertEquals


/**
 * Unit tests for [MsgClientAutoConfiguration].
 *
 * Only the pure [MsgClientAutoConfiguration.getComponentName] behavior is asserted here; full
 * Spring context wiring is exercised by the serial integration phase.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgClientAutoConfigurationTest {

    private val config = MsgClientAutoConfiguration()

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ms-msg-client", config.getComponentName())
    }

    @Test
    fun lifecycleHooks_runWithoutError() {
        val initializer: IComponentInitializer = config
        // Default no-arg hooks only log; assert they complete without throwing.
        initializer.beforeInit()
        initializer.afterInit()
    }
}
