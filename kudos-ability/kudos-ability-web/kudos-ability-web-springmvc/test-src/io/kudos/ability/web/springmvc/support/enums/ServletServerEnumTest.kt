package io.kudos.ability.web.springmvc.support.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Unit tests for [ServletServerEnum].
 *
 * Covers the enum entries (Undertow intentionally absent — unsupported by Spring Boot 4)
 * and valueOf resolution including the unknown-name failure path.
 *
 * @author K
 * @since 1.0.0
 */
internal class ServletServerEnumTest {

    @Test
    fun entries_containExactlyTomcatAndJetty() {
        assertEquals(listOf(ServletServerEnum.TOMCAT, ServletServerEnum.JETTY), ServletServerEnum.entries.toList())
    }

    @Test
    fun valueOf_resolvesByName() {
        assertEquals(ServletServerEnum.TOMCAT, ServletServerEnum.valueOf("TOMCAT"))
        assertEquals(ServletServerEnum.JETTY, ServletServerEnum.valueOf("JETTY"))
        assertFailsWith<IllegalArgumentException> { ServletServerEnum.valueOf("UNDERTOW") }
    }

}
