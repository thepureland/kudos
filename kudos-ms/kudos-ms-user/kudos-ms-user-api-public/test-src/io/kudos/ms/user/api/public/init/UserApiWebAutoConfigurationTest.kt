package io.kudos.ms.user.api.public.init

import io.kudos.context.init.IComponentInitializer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure unit test for [UserApiWebAutoConfiguration].
 *
 * No Spring context is started; the class is simply instantiated and its single behavior
 * ([UserApiWebAutoConfiguration.getComponentName]) is asserted. It implements
 * [IComponentInitializer], so [getComponentName] is invoked through that interface reference too.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserApiWebAutoConfigurationTest {

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ms-user-api-public", UserApiWebAutoConfiguration().getComponentName())
    }

    @Test
    fun getComponentName_viaInterfaceReference() {
        val initializer: IComponentInitializer = UserApiWebAutoConfiguration()
        assertEquals("kudos-ms-user-api-public", initializer.getComponentName())
    }
}
