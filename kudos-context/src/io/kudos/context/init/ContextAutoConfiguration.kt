package io.kudos.context.init

import io.kudos.context.spring.SpringContextInitializer
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered


/**
 * 上下文自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Import(ComponentInitializationDispatcher::class)
open class ContextAutoConfiguration : IComponentInitializer {

//    @Bean
//    @ConditionalOnMissingBean
//    open fun springContextHolder() = SpringContextInitializer()

    override fun getComponentName() = "kudos-context"

}