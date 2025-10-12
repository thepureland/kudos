package io.kudos.ability.web.springmvc.server

import io.kudos.ability.web.springmvc.support.enums.ServletServerEnum
import org.apache.catalina.connector.Connector
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory
import org.springframework.boot.web.server.WebServer
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * servlet容器工厂
 *
 *
 * 解决当同时存在多种servlet容器依赖时，可根据配置文件决定使用哪一种。
 * 可方便切换使用不同的容器，进行测试。
 *
 *
 * 当类路径中引入多种servlet容器依赖时，必须在application.yml或bootstrap.yml中添加以下属性配置：
 * spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration
 *
 *
 * 非单元测试环境，建议保证只存在一种servlet容器依赖！
 *
 * @author K
 * @since 1.0.0
 */
@Component("webServerFactory")
@Primary
class SwitchingServletWebServerFactory : AbstractServletWebServerFactory() {

    @Value($$"${kudos.ability.web.springmvc.server:TOMCAT}")
    private val servletServer: ServletServerEnum? = null

    @Value($$"${server.port:8080}")
    private val port = 8080

    @Value($$"${server.servlet.context-path:}")
    private val contextPath = ""

    override fun getWebServer(vararg initializers: ServletContextInitializer): WebServer {
        val serverFactory: AbstractServletWebServerFactory?
        when (servletServer) {
            ServletServerEnum.JETTY -> serverFactory = JettyServletWebServerFactory(port)
            ServletServerEnum.UNDERTOW -> serverFactory = UndertowServletWebServerFactory(port)
            else -> {
                serverFactory = TomcatServletWebServerFactory(port)
                serverFactory.addConnectorCustomizers({ connector: Connector? ->
                    // 解决用tomcat时，get请求传入特殊字符报400错误的问题
                    connector!!.setProperty("relaxedPathChars", "\"<>[\\]^`{|}")
                    connector.setProperty("relaxedQueryChars", "\"<>[\\]^`{|}")
                })
            }
        }
        if (contextPath.isNotBlank()) {
            serverFactory.contextPath = contextPath
        }
        return serverFactory.getWebServer(*initializers)
    }

}