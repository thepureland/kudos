package io.kudos.ms.user.api.internal.init

import io.kudos.context.init.IComponentInitializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test for [UserApiProviderAutoConfiguration].
 *
 * The class is a plain `@Configuration` exposing a single fixed component name; it is exercised here
 * by direct instantiation, with no Spring container started.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserApiProviderAutoConfigurationTest {

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ms-user-api-internal", UserApiProviderAutoConfiguration().getComponentName())
    }

    @Test
    fun isComponentInitializer() {
        assertTrue(UserApiProviderAutoConfiguration() is IComponentInitializer)
    }
}
