package io.kudos.ms.auth.api.internal.init

import io.kudos.context.init.IComponentInitializer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test for [AuthApiProviderAutoConfiguration].
 *
 * Only the pure, container-free contract is exercised here: [AuthApiProviderAutoConfiguration.getComponentName]
 * must return the stable component identifier, and the class must honour the [IComponentInitializer] contract.
 * Bean wiring / component scanning belongs to a full Spring context test and is deferred to the serial stage.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthApiProviderAutoConfigurationTest {

    private val config = AuthApiProviderAutoConfiguration()

    @Test
    fun getComponentName_returnsStableIdentifier() {
        assertEquals("kudos-ms-auth-api-internal", config.getComponentName())
    }

    @Test
    fun getComponentName_isDeterministicAcrossCalls() {
        assertEquals(config.getComponentName(), config.getComponentName())
    }

    @Test
    fun getComponentName_isReachableThroughInitializerContract() {
        // call via the IComponentInitializer reference to exercise the overridden contract method
        val initializer: IComponentInitializer = config
        assertEquals("kudos-ms-auth-api-internal", initializer.getComponentName())
    }
}
