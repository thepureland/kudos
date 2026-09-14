package io.kudos.ms.tag.api.internal.init

import kotlin.test.Test
import kotlin.test.assertEquals

internal class TagApiInternalAutoConfigurationTest {

    @Test
    fun getComponentNameReturnsModuleName() {
        assertEquals("kudos-ms-tag-api-internal", TagApiInternalAutoConfiguration().getComponentName())
    }
}
