package io.kudos.ability.web.springmvc.init

import org.springframework.boot.SpringApplicationRunListener
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext
import org.springframework.context.ConfigurableApplicationContext

class CustomSpringApplicationRunListener : SpringApplicationRunListener {

    override fun contextPrepared(context: ConfigurableApplicationContext?) {
        if (context is AnnotationConfigServletWebServerApplicationContext) {
            val customContext = ServletWebServerApplicationContext()
            customContext.setServletContext(context.getServletContext())
            customContext.namespace = context.namespace
            customContext.id = context.getId()
            customContext.environment = context.getEnvironment()
            customContext.parent = context.getParent()
            customContext.classLoader = context.getClassLoader()
//            customContext.setResourceLoader(context.getResourceLoader())
        }
    }

}