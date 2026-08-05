package io.kudos.ms.msg.api.internal.init

import io.kudos.context.init.IComponentInitializer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for MsgApiProviderAutoConfiguration (pure instantiation, no Spring container)
 *
 * Coverage:
 * - getComponentName returns the fixed module name
 * - getComponentName invoked through the IComponentInitializer contract returns the same value
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgApiProviderAutoConfigurationTest {

    @Test
    fun getComponentName_returnsModuleName() {
        val config = MsgApiProviderAutoConfiguration()

        assertEquals("kudos-ms-msg-api-internal", config.getComponentName())
    }

    @Test
    fun getComponentName_viaInterfaceContract() {
        val initializer: IComponentInitializer = MsgApiProviderAutoConfiguration()

        assertEquals("kudos-ms-msg-api-internal", initializer.getComponentName())
    }
}
