package io.kudos.ms.auth.api.admin.init

import io.kudos.context.init.IComponentInitializer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test for [AuthApiAdminAutoConfiguration] (pure; no Spring container started).
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthApiAdminAutoConfigurationTest {

    @Test
    fun getComponentName_returnsModuleName() {
        val config = AuthApiAdminAutoConfiguration()
        assertEquals("kudos-ms-auth-api-admin", config.getComponentName())
    }

    @Test
    fun isComponentInitializer() {
        // Compile-time-typed reference verifies the class implements the initializer contract.
        val initializer: IComponentInitializer = AuthApiAdminAutoConfiguration()
        assertEquals("kudos-ms-auth-api-admin", initializer.getComponentName())
    }

    @Test
    fun applicationClassInstantiable() {
        // Bootstrap marker class with no behaviour; ensure it constructs.
        AuthApiAdminApplication()
    }
}
