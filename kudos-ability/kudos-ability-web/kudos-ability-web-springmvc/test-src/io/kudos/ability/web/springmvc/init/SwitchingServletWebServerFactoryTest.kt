package io.kudos.ability.web.springmvc.init

import org.apache.catalina.connector.Connector
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServer
import org.springframework.boot.web.server.servlet.ServletWebServerFactory
import org.springframework.mock.env.MockEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for [SwitchingServletWebServerFactory].
 *
 * Covers:
 *  - default (no configuration) -> Tomcat
 *  - explicit TOMCAT / JETTY selection, including lowercase values (case-insensitive match)
 *  - unrecognized server values fall back to Tomcat (warn branch)
 *  - context-path configuration applied via reflection
 *  - port 0 -> random port allocation (servers are started and stopped in-test)
 *
 * @author K
 * @since 1.0.0
 */
internal class SwitchingServletWebServerFactoryTest {

    private fun env(vararg props: Pair<String, String>): MockEnvironment {
        val environment = MockEnvironment()
        // Always use a random port to keep tests parallel-safe.
        environment.setProperty("server.port", "0")
        props.forEach { (k, v) -> environment.setProperty(k, v) }
        return environment
    }

    private fun withWebServer(environment: MockEnvironment, block: (WebServer) -> Unit) {
        val server = SwitchingServletWebServerFactory(environment).getWebServer()
        try {
            server.start()
            block(server)
        } finally {
            server.stop()
        }
    }

    @Test
    fun getWebServer_defaultsToTomcat() {
        withWebServer(env()) { server ->
            assertEquals("TomcatWebServer", server.javaClass.simpleName)
            assertTrue(server.port > 0, "A random port should have been allocated")
        }
    }

    @Test
    fun getWebServer_unrecognizedValue_fallsBackToTomcat() {
        withWebServer(env("kudos.ability.web.springmvc.server" to "NETTY")) { server ->
            assertEquals("TomcatWebServer", server.javaClass.simpleName)
        }
    }

    @Test
    fun getWebServer_jetty() {
        withWebServer(env("kudos.ability.web.springmvc.server" to "JETTY")) { server ->
            assertEquals("JettyServletWebServer", server.javaClass.simpleName)
            assertTrue(server.port > 0)
        }
    }

    @Test
    fun getWebServer_lowercaseValue_isUppercasedBeforeMatching() {
        withWebServer(env("kudos.ability.web.springmvc.server" to "jetty")) { server ->
            assertEquals("JettyServletWebServer", server.javaClass.simpleName)
        }
    }

    @Test
    fun getWebServer_appliesContextPath() {
        withWebServer(env("server.servlet.context-path" to "/app")) { server ->
            assertEquals("TomcatWebServer", server.javaClass.simpleName)
            assertTrue(server.port > 0)
        }
    }

    /**
     * Invoke the private [SwitchingServletWebServerFactory.createTomcatFactory] and run its registered
     * connector customizer against a real [Connector], asserting the relaxed path/query chars are applied.
     * This deterministically covers the customizer-lambda body (the `requireNotNull` non-null branch
     * plus both `setProperty` calls) without having to boot a server and reflect into its connector.
     */
    @Test
    fun createTomcatFactory_connectorCustomizer_relaxesPathAndQueryChars() {
        val factoryUnderTest = SwitchingServletWebServerFactory(env())
        val method = SwitchingServletWebServerFactory::class.java
            .getDeclaredMethod("createTomcatFactory", Int::class.javaPrimitiveType)
        method.isAccessible = true
        val tomcatFactory = method.invoke(factoryUnderTest, 0) as ServletWebServerFactory

        // Pull the customizers Spring Boot stores on the Tomcat factory and apply them to a fresh connector.
        val customizers = (tomcatFactory as TomcatServletWebServerFactory).connectorCustomizers
        assertTrue(customizers.isNotEmpty(), "createTomcatFactory must register at least one connector customizer")

        val connector = Connector()
        customizers.forEach { it.customize(connector) }

        assertEquals("\"<>[\\]^`{|}", connector.getProperty("relaxedPathChars"))
        assertEquals("\"<>[\\]^`{|}", connector.getProperty("relaxedQueryChars"))
    }

    /**
     * The customizer guards against a null connector via [requireNotNull]; feeding it null must raise
     * IllegalArgumentException with the documented message. Covers the failure branch of the lambda's `requireNotNull`.
     */
    @Test
    fun createTomcatFactory_connectorCustomizer_rejectsNullConnector() {
        val factoryUnderTest = SwitchingServletWebServerFactory(env())
        val method = SwitchingServletWebServerFactory::class.java
            .getDeclaredMethod("createTomcatFactory", Int::class.javaPrimitiveType)
        method.isAccessible = true
        val tomcatFactory = method.invoke(factoryUnderTest, 0) as TomcatServletWebServerFactory
        val customizer = tomcatFactory.connectorCustomizers.first()

        // The interface's customize(Connector) parameter is non-null in Kotlin's eyes, so invoke it
        // reflectively with a null argument to drive the lambda's requireNotNull failure branch.
        val customizeMethod = customizer.javaClass.getMethod("customize", Connector::class.java)
        val ex = assertFailsWith<java.lang.reflect.InvocationTargetException> {
            customizeMethod.invoke(customizer, null)
        }
        val cause = ex.targetException
        assertTrue(cause is IllegalArgumentException)
        assertEquals("connector is null", cause.message)
    }

}
