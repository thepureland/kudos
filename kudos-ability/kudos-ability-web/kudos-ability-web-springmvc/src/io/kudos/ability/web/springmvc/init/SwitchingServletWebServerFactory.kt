package io.kudos.ability.web.springmvc.init

import io.kudos.ability.web.springmvc.support.enums.ServletServerEnum
import org.apache.catalina.connector.Connector
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServer
import org.springframework.boot.web.server.servlet.ServletWebServerFactory
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.core.env.getProperty
import org.springframework.stereotype.Component

/**
 * Servlet container factory.
 *
 *
 * Solves the case where multiple servlet container dependencies are present: the configuration file decides which one to use.
 * Makes it easy to switch between different containers for testing.
 *
 *
 * Outside unit-test environments, it is recommended to keep only a single servlet container dependency!
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component("webServerFactory")
@Primary
class SwitchingServletWebServerFactory(
    private val env: Environment
) : ServletWebServerFactory {

    /**
     * Pick the concrete container factory based on yml configuration; unrecognised values fall back to Tomcat. Flow:
     * read config -> build factory -> set contextPath via reflection (factory signatures differ) -> delegate the actual [WebServer] construction.
     */
    override fun getWebServer(vararg initializers: ServletContextInitializer): WebServer {
        val serverTypeStr = env.getProperty("kudos.ability.web.springmvc.server", "TOMCAT")
        val serverType = try {
            ServletServerEnum.valueOf(serverTypeStr.uppercase())
        } catch (ex: IllegalArgumentException) {
            ServletServerEnum.TOMCAT
        }
        val port = env.getProperty<Int>("server.port", 8080)
        val contextPath = env.getProperty("server.servlet.context-path", "")

        val serverFactory = when (serverType) {
            ServletServerEnum.JETTY ->  createJettyFactory(port)
            else -> createTomcatFactory(port)
        }

        if (contextPath.isNotBlank()) {
            try {
                val method = serverFactory.javaClass.getMethod("setContextPath", String::class.java)
                method.invoke(serverFactory, contextPath)
            } catch (_: NoSuchMethodException) {
                // Some factory implementations do not have this method; ignore.
            }
        }

        return serverFactory.getWebServer(*initializers)
    }

    /**
     * Construct a [TomcatServletWebServerFactory] and relax the Connector's relaxedPathChars /
     * relaxedQueryChars to avoid Tomcat returning 400 for GET requests containing characters like `"<>[]\^` `{|}`.
     *
     * @param port listening port
     * @return the configured Tomcat factory
     * @author K
     * @since 1.0.0
     */
    private fun createTomcatFactory(port: Int): ServletWebServerFactory {
        val serverFactory = TomcatServletWebServerFactory(port)
        serverFactory.addConnectorCustomizers({ connector: Connector? ->
            val c = requireNotNull(connector) { "connector is null" }
            c.setProperty("relaxedPathChars", "\"<>[\\]^`{|}")
            c.setProperty("relaxedQueryChars", "\"<>[\\]^`{|}")
        })
        return serverFactory
    }

    /**
     * Load the Jetty factory via reflection: this module only `testImplementation`s spring-boot-starter-jetty,
     * so a direct import would cause compile/start-up failures in production where Jetty is absent. Use `Class.forName`
     * for runtime resolution instead.
     *
     * @param port listening port
     * @return the configured Jetty factory
     * @throws ClassNotFoundException when Jetty is not on the classpath (only triggered if the configuration selects JETTY)
     * @author K
     * @since 1.0.0
     */
    private fun createJettyFactory(port: Int): ServletWebServerFactory {
        val fqcn = "org.springframework.boot.jetty.servlet.JettyServletWebServerFactory"
        val clazz = Class.forName(fqcn)
        val ctor = clazz.getConstructor(Int::class.javaPrimitiveType)
        val instance = ctor.newInstance(port)
        return instance as ServletWebServerFactory
    }

}