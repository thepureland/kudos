package io.kudos.ms.auth.core.platform.init

import io.kudos.context.init.IComponentInitializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure unit test for [AuthAutoConfiguration]: the auto-configuration is an [IComponentInitializer]
 * whose component name identifies this atomic service. No Spring context is started — only the
 * component-name contract is asserted, since the configuration's wiring is exercised by every
 * container-backed integration test in this module.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthAutoConfigurationTest {

    @Test
    fun getComponentName_returnsModuleName() {
        val cfg = AuthAutoConfiguration()
        assertTrue(cfg is IComponentInitializer)
        assertEquals("kudos-ms-auth-core", cfg.getComponentName())
    }
}
