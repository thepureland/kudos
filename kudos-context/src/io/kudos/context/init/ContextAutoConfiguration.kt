package io.kudos.context.init

import io.kudos.base.logger.LoggerFactory
import org.soul.context.context.SoulContextBeanDefinitionRegistrar
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.*
import org.springframework.core.Ordered
import javax.annotation.PostConstruct


@Import(SoulContextBeanDefinitionRegistrar::class)
@ComponentScan(basePackages = ["io.kudos.context"])
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration // 会使用cglib,碰巧使该类比较早被实例化
open class ContextAutoConfiguration : IComponentInitializer {

    private val logger = LoggerFactory.getLogger(this)

    @PostConstruct
    override fun init() {
        logger.info("【kudos-context】初始化完成.")
    }

}