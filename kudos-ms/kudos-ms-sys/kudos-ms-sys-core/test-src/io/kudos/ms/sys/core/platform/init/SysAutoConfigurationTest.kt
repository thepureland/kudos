package io.kudos.ms.sys.core.platform.init

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for SysAutoConfiguration (pure logic, no Spring container)
 *
 * Coverage:
 * - getComponentName returns the fixed component name "kudos-ms-sys-core"
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAutoConfigurationTest {

    @Test
    fun getComponentName() {
        assertEquals("kudos-ms-sys-core", SysAutoConfiguration().getComponentName())
    }
}
