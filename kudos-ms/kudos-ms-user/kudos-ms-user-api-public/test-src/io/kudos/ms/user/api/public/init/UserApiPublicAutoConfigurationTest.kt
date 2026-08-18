package io.kudos.ms.user.api.public.init

import io.kudos.context.init.IComponentInitializer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure unit test for [UserApiPublicAutoConfiguration].
 *
 * No Spring context is started; the class is simply instantiated and its single behavior
 * ([UserApiPublicAutoConfiguration.getComponentName]) is asserted. It implements
 * [IComponentInitializer], so [getComponentName] is invoked through that interface reference too.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserApiPublicAutoConfigurationTest {

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ms-user-api-public", UserApiPublicAutoConfiguration().getComponentName())
    }

    @Test
    fun getComponentName_viaInterfaceReference() {
        val initializer: IComponentInitializer = UserApiPublicAutoConfiguration()
        assertEquals("kudos-ms-user-api-public", initializer.getComponentName())
    }
}
