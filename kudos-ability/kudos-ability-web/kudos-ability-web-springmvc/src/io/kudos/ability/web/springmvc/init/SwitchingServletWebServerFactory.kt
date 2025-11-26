package io.kudos.ability.web.springmvc.init

import io.kudos.ability.web.springmvc.support.enums.ServletServerEnum
import org.apache.catalina.connector.Connector
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServer
import org.springframework.boot.web.server.servlet.ServletWebServerFactory
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * servlet容器工厂
 *
 *
 * 解决当同时存在多种servlet容器依赖时，可根据配置文件决定使用哪一种。
 * 可方便切换使用不同的容器，进行测试。
 *
 *
 * 非单元测试环境，建议保证只存在一种servlet容器依赖！
 *
 * @author K
 * @since 1.0.0
 */
@Component("webServerFactory")
@Primary
class SwitchingServletWebServerFactory(
    private val env: Environment
) : ServletWebServerFactory {

    override fun getWebServer(vararg initializers: ServletContextInitializer): WebServer {
        // 1. 读取配置
        val serverTypeStr = env.getProperty("kudos.ability.web.springmvc.server", "TOMCAT")
        val serverType = try {
            ServletServerEnum.valueOf(serverTypeStr.uppercase())
        } catch (ex: IllegalArgumentException) {
            ServletServerEnum.TOMCAT
        }
        val port = env.getProperty("server.port", Int::class.java, 8080)
        val contextPath = env.getProperty("server.servlet.context-path", "")

        // 2. 创建具体工厂
        val serverFactory = when (serverType) {
            ServletServerEnum.JETTY ->  createJettyFactory(port)
            else -> createTomcatFactory(port)
        }

        // 3. 设置 contextPath
        if (contextPath.isNotBlank()) {
            try {
                val method = serverFactory.javaClass.getMethod("setContextPath", String::class.java)
                method.invoke(serverFactory, contextPath)
            } catch (_: NoSuchMethodException) {
                // 有的实现可能没有这个方法，忽略即可
            }
        }

        // 4. 返回真正的 WebServer
        return serverFactory.getWebServer(*initializers)
    }

    private fun createTomcatFactory(port: Int): ServletWebServerFactory {
        val serverFactory = TomcatServletWebServerFactory(port)
        serverFactory.addConnectorCustomizers({ connector: Connector? ->
            // 解决用tomcat时，get请求传入特殊字符报400错误的问题
            connector!!.setProperty("relaxedPathChars", "\"<>[\\]^`{|}")
            connector.setProperty("relaxedQueryChars", "\"<>[\\]^`{|}")
        })
        return serverFactory
    }

    private fun createJettyFactory(port: Int): ServletWebServerFactory {
        val fqcn = "org.springframework.boot.jetty.servlet.JettyServletWebServerFactory"
        val clazz = Class.forName(fqcn)
        val ctor = clazz.getConstructor(Int::class.javaPrimitiveType)
        val instance = ctor.newInstance(port)
        return instance as ServletWebServerFactory
    }

}