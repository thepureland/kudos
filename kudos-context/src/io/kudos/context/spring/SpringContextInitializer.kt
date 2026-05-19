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

    /**
     * 把 Spring 启动注入进来的 [ConfigurableApplicationContext] 注册到 [SpringKit]，
     * 后续 [SpringKit.getBean] / [SpringKit.getBeansOfType] 等门面才能在静态上下文里取到 bean。
     *
     * 注：本初始化器需在 META-INF/spring.factories 或 SpringApplication.addInitializers 中显式注册；
     * 必须先于任何依赖 [SpringKit] 的 `by lazy` / `object init` 代码被触发。
     *
     * @param applicationContext Spring 提供的可配置上下文
     * @author K
     * @since 1.0.0
     */
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        SpringKit.applicationContext = applicationContext
    }

}