package io.kudos.ability.web.ktor.init

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for KtorProperties
 *
 * 覆盖：
 * 1. 默认值（engine.name=test, port=0, 各插件默认启用）
 * 2. 可变属性的修改
 * 3. data class 的 equals/copy 语义
 *
 * @author K
 * @since 1.0.0
 */
internal class KtorPropertiesTest {

    @Test
    fun defaults() {
        val p = KtorProperties()
        assertEquals("test", p.engine.name)
        assertEquals(0, p.engine.port)
        assertTrue(p.plugins.contentNegotiation.enabled)
        assertTrue(p.plugins.statusPages.enabled)
        assertTrue(p.plugins.webSocket.enabled)
    }

    @Test
    fun mutableProperties_canBeChanged() {
        val p = KtorProperties()
        p.engine.name = "netty"
        p.engine.port = 65535
        p.plugins.contentNegotiation = KtorProperties.Plugins.Plugin(enabled = false)
        p.plugins.statusPages.enabled = false
        p.plugins.webSocket.enabled = false

        assertEquals("netty", p.engine.name)
        assertEquals(65535, p.engine.port)
        assertFalse(p.plugins.contentNegotiation.enabled)
        assertFalse(p.plugins.statusPages.enabled)
        assertFalse(p.plugins.webSocket.enabled)
    }

    @Test
    fun dataClassSemantics() {
        val p1 = KtorProperties()
        val p2 = KtorProperties()
        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())

        val p3 = p1.copy(engine = KtorProperties.Engine(name = "cio", port = 8080))
        assertNotEquals(p1, p3)
        assertEquals("cio", p3.engine.name)
        assertEquals(8080, p3.engine.port)
        // copy 不影响原对象
        assertEquals("test", p1.engine.name)
    }

}
