package io.kudos.context.spring

import io.kudos.context.kit.SpringKit
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Spring上下文持有者
 *
 * @author K
 * @since 1.0.0
 */
class SpringContextHolder : ApplicationContextAware {

    private lateinit var applicationContext: ApplicationContext

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
        SpringKit.applicationContext = applicationContext
    }

    fun getApplicationContext(): ApplicationContext = applicationContext

}