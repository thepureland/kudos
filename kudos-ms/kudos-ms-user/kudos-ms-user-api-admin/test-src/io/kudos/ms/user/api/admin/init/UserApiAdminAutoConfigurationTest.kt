package io.kudos.ms.user.api.admin.init

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test for [UserApiAdminAutoConfiguration]; no Spring container is started.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserApiAdminAutoConfigurationTest {

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ms-user-api-admin", UserApiAdminAutoConfiguration().getComponentName())
    }

    @Test
    fun instantiable() {
        assertNotNull(UserApiAdminAutoConfiguration())
    }

}
