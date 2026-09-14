package io.kudos.ms.tag.api.public.init

import kotlin.test.Test
import kotlin.test.assertEquals

internal class TagApiPublicAutoConfigurationTest {

    @Test
    fun getComponentNameReturnsModuleName() {
        assertEquals("kudos-ms-tag-api-public", TagApiPublicAutoConfiguration().getComponentName())
    }
}
