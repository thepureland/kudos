package io.kudos.context.kit

import org.springframework.aop.framework.ProxyFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for ProxyKit
 *
 * Coverage:
 * - A plain (non-proxy) object returns its own class
 * - A JDK dynamic proxy (interface-based) resolves to the real target class
 * - A CGLIB proxy (class-based) resolves to the real target class
 * - Nested proxies (proxy of a proxy) resolve to the ultimate target class
 *
 * @author K
 * @since 1.0.0
 */
internal class ProxyKitTest {

    interface Greeter {
        fun greet(): String
    }

    internal open class GreeterImpl : Greeter {
        override fun greet() = "hi"
    }

    @Test
    fun plainObjectReturnsItsOwnClass() {
        assertEquals(GreeterImpl::class, ProxyKit.getTargetClass(GreeterImpl()))
        assertEquals(String::class, ProxyKit.getTargetClass("plain string"))
    }

    @Test
    fun jdkDynamicProxyResolvesToTargetClass() {
        val target = GreeterImpl()
        val proxy = ProxyFactory(target).apply {
            setInterfaces(Greeter::class.java)
        }.proxy
        assertTrue(java.lang.reflect.Proxy.isProxyClass(proxy.javaClass), "precondition: should be a JDK proxy")
        assertEquals(GreeterImpl::class, ProxyKit.getTargetClass(proxy))
    }

    @Test
    fun cglibProxyResolvesToTargetClass() {
        val target = GreeterImpl()
        val proxy = ProxyFactory(target).apply {
            isProxyTargetClass = true
        }.proxy
        assertTrue(proxy.javaClass.name.contains("\$\$"), "precondition: should be a CGLIB subclass proxy")
        assertEquals(GreeterImpl::class, ProxyKit.getTargetClass(proxy))
    }

    @Test
    fun nestedProxyResolvesToUltimateTargetClass() {
        val target = GreeterImpl()
        val inner = ProxyFactory(target).apply { setInterfaces(Greeter::class.java) }.proxy
        val outer = ProxyFactory(inner).apply { setInterfaces(Greeter::class.java) }.proxy
        assertEquals(GreeterImpl::class, ProxyKit.getTargetClass(outer), "nested proxies should see through to the ultimate target")
    }
}
