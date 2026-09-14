package io.kudos.ms.tag.core.platform.init

import kotlin.test.Test
import kotlin.test.assertEquals

internal class TagAutoConfigurationTest {

    @Test
    fun getComponentNameReturnsModuleName() {
        assertEquals("kudos-ms-tag-core", TagAutoConfiguration().getComponentName())
    }
}
