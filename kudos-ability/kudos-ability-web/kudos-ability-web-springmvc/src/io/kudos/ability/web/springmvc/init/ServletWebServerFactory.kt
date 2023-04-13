package io.kudos.ability.web.springmvc.init

import org.apache.catalina.connector.Connector
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory
import org.springframework.boot.web.server.WebServer
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory

/**
 * servlet容器工厂
 *
 * 解决当同时存在多种servlet容器依赖时，可根据配置文件决定使用哪一种。
 * 可方便切换使用不同的容器，进行测试。
 *
 * @author K
 * @since 5.0.0
 */
open class ServletWebServerFactory : AbstractServletWebServerFactory() {

    @Value("\${soul.ability.web.springmvc.server:TOMCAT}")
    private val servletServer: ServletServerEnum? = null

    override fun getWebServer(vararg initializers: ServletContextInitializer): WebServer {
        return when (servletServer) {
            ServletServerEnum.JETTY -> {
                JettyServletWebServerFactory().getWebServer(*initializers)
            }
            ServletServerEnum.UNDERTOW -> {
                return UndertowServletWebServerFactory().getWebServer(*initializers)
            }
            else -> {
                val factory = TomcatServletWebServerFactory()
                factory.addConnectorCustomizers(TomcatConnectorCustomizer { connector: Connector ->
                    // 解决用tomcat时，get请求传入特殊字符报400错误的问题
                    connector.setProperty("relaxedPathChars", "\"<>[\\]^`{|}")
                    connector.setProperty("relaxedQueryChars", "\"<>[\\]^`{|}")
                })
                return factory.getWebServer(*initializers)
            }
        }
    }

}