package io.kudos.ability.web.springmvc.init

import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext
import org.springframework.boot.web.servlet.server.ServletWebServerFactory

open class ServletWebServerApplicationContext : AnnotationConfigServletWebServerApplicationContext() {

    override fun getWebServerFactory(): ServletWebServerFactory {
        return super.getWebServerFactory()
    }

}