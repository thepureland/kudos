package io.kudos.ms.tag.api.admin.init

import kotlin.test.Test
import kotlin.test.assertEquals

internal class TagApiAdminAutoConfigurationTest {

    @Test
    fun getComponentNameReturnsModuleName() {
        assertEquals("kudos-ms-tag-api-admin", TagApiAdminAutoConfiguration().getComponentName())
    }
}
