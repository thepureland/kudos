package io.kudos.ms.msg.core.platform.init

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure unit test for [MsgAutoConfiguration] — verifies the component name contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgAutoConfigurationTest {

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ms-msg-core", MsgAutoConfiguration().getComponentName())
    }
}
