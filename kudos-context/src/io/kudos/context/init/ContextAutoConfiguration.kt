package io.kudos.context.init

import io.kudos.base.logger.LoggerFactory
import org.soul.context.context.SoulContextBeanDefinitionRegistrar
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.*
import org.springframework.core.Ordered
import javax.annotation.PostConstruct


@Import(SoulContextBeanDefinitionRegistrar::class)
@ComponentScan(
    basePackages = ["io.kudos.context"],
    excludeFilters = [ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = [ContextAutoConfiguration::class]
    )]
)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration
open class ContextAutoConfiguration {

    private val logger = LoggerFactory.getLogger(this)

    @Bean("string121212")
    @ConditionalOnMissingBean
    open fun string1212(): Pair<String, String> {
        logger.info("ContextAutoConfiguration::string()")
        return Pair("1", "2")
    }

    @PostConstruct
    open fun init() {
        logger.info("【kudos-context】初始化完成.")
    }

}