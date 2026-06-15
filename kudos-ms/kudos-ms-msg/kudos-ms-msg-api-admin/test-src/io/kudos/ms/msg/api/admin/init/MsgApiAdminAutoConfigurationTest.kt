package io.kudos.ms.msg.api.admin.init

import io.kudos.context.init.IComponentInitializer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure unit test for [MsgApiAdminAutoConfiguration] (no Spring container started).
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgApiAdminAutoConfigurationTest {

    private val config: IComponentInitializer = MsgApiAdminAutoConfiguration()

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ms-msg-api-admin", config.getComponentName())
    }
}
