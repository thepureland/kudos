package io.kudos.ability.comm.email.init

import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.soul.ability.comm.email.handler.EmailHandler
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean


/**
 * 邮件发送自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configurable
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class EmailAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean
    open fun emailHandler() = EmailHandler()

    override fun getComponentName() = "kudos-ability-comm-email"

}