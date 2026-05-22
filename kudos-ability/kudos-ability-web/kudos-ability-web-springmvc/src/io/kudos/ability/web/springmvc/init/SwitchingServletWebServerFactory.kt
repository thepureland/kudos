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
 * @author AI: Codex
 * @since 1.0.0
 */
@Component("webServerFactory")
@Primary
class SwitchingServletWebServerFactory(
    private val env: Environment
) : ServletWebServerFactory {

    /**
     * 按 yml 配置选择具体的容器工厂；未识别值回落 Tomcat。流程：读配置 → 建工厂 →
     * 反射设置 contextPath（不同工厂签名不同）→ 委托真正的 [WebServer] 构建。
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
                // 部分工厂实现无此方法，忽略即可
            }
        }

        return serverFactory.getWebServer(*initializers)
    }

    /**
     * 构造 [TomcatServletWebServerFactory]，并放宽 Connector 的 relaxedPathChars /
     * relaxedQueryChars，避免 GET 请求带 `"<>[]\^` `{|}` 等字符时 Tomcat 直接 400。
     *
     * @param port 监听端口
     * @return 配置完的 Tomcat 工厂
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
     * 反射加载 Jetty 工厂——本模块依赖里只 testImplementation 了 spring-boot-starter-jetty，
     * 直接 import 会让生产侧没有 Jetty 时编译/启动失败。改用 `Class.forName` 走运行期解析。
     *
     * @param port 监听端口
     * @return 配置完的 Jetty 工厂
     * @throws ClassNotFoundException 类路径下没有 Jetty 时（仅当配置选择 JETTY 时才会触发）
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