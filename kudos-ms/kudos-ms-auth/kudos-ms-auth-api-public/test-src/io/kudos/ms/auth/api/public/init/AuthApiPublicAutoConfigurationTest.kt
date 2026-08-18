package io.kudos.ms.auth.api.public.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pure unit test for [AuthApiPublicAutoConfiguration] — verifies the component-name contract and the
 * Spring stereotype/scan annotations without booting any Spring container.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthApiPublicAutoConfigurationTest {

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ms-auth-api-public", AuthApiPublicAutoConfiguration().getComponentName())
    }

    @Test
    fun getComponentName_isStableAcrossCalls() {
        val config = AuthApiPublicAutoConfiguration()
        val first = config.getComponentName()
        val second = config.getComponentName()
        assertEquals(first, second)
        // returns the very same interned string literal each time
        assertSame(first, second)
    }

    @Test
    fun getComponentName_isStableAcrossInstances() {
        assertEquals(
            AuthApiPublicAutoConfiguration().getComponentName(),
            AuthApiPublicAutoConfiguration().getComponentName()
        )
    }

    @Test
    fun isComponentInitializer() {
        // the class must honour the IComponentInitializer contract that drives component scanning
        assertTrue(
            IComponentInitializer::class.java.isAssignableFrom(AuthApiPublicAutoConfiguration::class.java)
        )
    }

    @Test
    fun defaultBeforeInitAndAfterInit_doNotThrow() {
        // exercise the inherited default lifecycle hooks (they only log) through this concrete impl
        val config = AuthApiPublicAutoConfiguration()
        config.beforeInit()
        config.afterInit()
    }

    @Test
    fun carriesConfigurationAnnotation() {
        assertNotNull(
            AuthApiPublicAutoConfiguration::class.java.getAnnotation(Configuration::class.java),
            "must be annotated with @Configuration"
        )
    }

    @Test
    fun carriesComponentScanForOwnBasePackage() {
        val scan = AuthApiPublicAutoConfiguration::class.java.getAnnotation(ComponentScan::class.java)
        assertNotNull(scan, "must be annotated with @ComponentScan")
        assertEquals(listOf("io.kudos.ms.auth.api.public"), scan.basePackages.toList())
    }
}
