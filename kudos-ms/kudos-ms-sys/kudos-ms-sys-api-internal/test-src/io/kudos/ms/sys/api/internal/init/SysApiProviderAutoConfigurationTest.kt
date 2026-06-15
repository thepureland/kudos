package io.kudos.ms.sys.api.internal.init

import io.kudos.context.init.IComponentInitializer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for SysApiProviderAutoConfiguration (pure instantiation, no Spring container)
 *
 * Coverage:
 * - getComponentName returns the fixed module name
 * - getComponentName invoked through the IComponentInitializer contract returns the same value
 *
 * @author K
 * @since 1.0.0
 */
internal class SysApiProviderAutoConfigurationTest {

    @Test
    fun getComponentName_returnsModuleName() {
        val config = SysApiProviderAutoConfiguration()

        assertEquals("kudos-ms-sys-api-internal", config.getComponentName())
    }

    @Test
    fun getComponentName_viaInterfaceContract() {
        val initializer: IComponentInitializer = SysApiProviderAutoConfiguration()

        assertEquals("kudos-ms-sys-api-internal", initializer.getComponentName())
    }
}
