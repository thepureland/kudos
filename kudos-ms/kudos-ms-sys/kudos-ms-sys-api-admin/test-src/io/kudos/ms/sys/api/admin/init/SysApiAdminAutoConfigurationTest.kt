package io.kudos.ms.sys.api.admin.init

import io.kudos.context.init.IComponentInitializer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure unit test for [SysApiAdminAutoConfiguration.getComponentName]; no Spring context needed —
 * the config object is constructed directly and only its component-name contract is asserted.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysApiAdminAutoConfigurationTest {

    @Test
    fun getComponentNameReturnsModuleName() {
        val config = SysApiAdminAutoConfiguration()
        assertEquals("kudos-ms-sys-api-admin", config.getComponentName())
    }

    @Test
    fun isAComponentInitializerWithSameComponentNameContract() {
        // Used via the IComponentInitializer contract; assert through the interface type.
        val initializer: IComponentInitializer = SysApiAdminAutoConfiguration()
        assertEquals("kudos-ms-sys-api-admin", initializer.getComponentName())
    }
}
