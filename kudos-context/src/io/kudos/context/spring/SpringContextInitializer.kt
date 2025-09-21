package io.kudos.context.spring

import io.kudos.context.kit.SpringKit
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

/**
 * Spring上下文初始化器
 *
 * @author K
 * @since 1.0.0
 */
class SpringContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        SpringKit.applicationContext = applicationContext
    }

}