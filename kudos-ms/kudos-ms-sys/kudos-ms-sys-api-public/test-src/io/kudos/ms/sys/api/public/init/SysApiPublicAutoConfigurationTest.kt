package io.kudos.ms.sys.api.public.init

import io.kudos.context.init.IComponentInitializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pure unit test for [SysApiPublicAutoConfiguration] — verifies the component-name contract
 * without booting any Spring container.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysApiPublicAutoConfigurationTest {

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ms-sys-api-public", SysApiPublicAutoConfiguration().getComponentName())
    }

    @Test
    fun getComponentName_isStableAcrossCalls() {
        val config = SysApiPublicAutoConfiguration()
        val first = config.getComponentName()
        val second = config.getComponentName()
        assertEquals(first, second)
        // returns the very same interned string literal each time
        assertSame(first, second)
    }

    @Test
    fun getComponentName_isStableAcrossInstances() {
        assertEquals(
            SysApiPublicAutoConfiguration().getComponentName(),
            SysApiPublicAutoConfiguration().getComponentName()
        )
    }

    @Test
    fun isComponentInitializer() {
        // the class must honour the IComponentInitializer contract that drives component scanning
        assertTrue(
            IComponentInitializer::class.java.isAssignableFrom(SysApiPublicAutoConfiguration::class.java)
        )
    }
}
