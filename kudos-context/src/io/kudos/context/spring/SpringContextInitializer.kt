package io.kudos.context.spring

import io.kudos.context.kit.SpringKit
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

/**
 * Spring application context initializer.
 *
 * @author K
 * @since 1.0.0
 */
class SpringContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    /**
     * Registers the [ConfigurableApplicationContext] injected by Spring on startup into [SpringKit],
     * so that facades like [SpringKit.getBean] / [SpringKit.getBeansOfType] can retrieve beans from the
     * static context.
     *
     * Note: this initializer must be registered explicitly in META-INF/spring.factories or via
     * SpringApplication.addInitializers, and must run before any `by lazy` / `object init` code that
     * depends on [SpringKit].
     *
     * @param applicationContext the configurable context provided by Spring
     * @author K
     * @since 1.0.0
     */
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        SpringKit.applicationContext = applicationContext
    }

}