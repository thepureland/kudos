package io.kudos.ms.msg.api.public.init

import io.kudos.context.init.IComponentInitializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pure unit test for [MsgApiPublicAutoConfiguration] — verifies the component-name contract
 * without booting any Spring container.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgApiPublicAutoConfigurationTest {

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ms-msg-api-public", MsgApiPublicAutoConfiguration().getComponentName())
    }

    @Test
    fun getComponentName_isStableAcrossCalls() {
        val config = MsgApiPublicAutoConfiguration()
        val first = config.getComponentName()
        val second = config.getComponentName()
        assertEquals(first, second)
        // returns the very same interned string literal each time
        assertSame(first, second)
    }

    @Test
    fun getComponentName_isStableAcrossInstances() {
        assertEquals(
            MsgApiPublicAutoConfiguration().getComponentName(),
            MsgApiPublicAutoConfiguration().getComponentName()
        )
    }

    @Test
    fun isComponentInitializer() {
        // the class must honour the IComponentInitializer contract that drives component scanning
        assertTrue(
            IComponentInitializer::class.java.isAssignableFrom(MsgApiPublicAutoConfiguration::class.java)
        )
    }
}
