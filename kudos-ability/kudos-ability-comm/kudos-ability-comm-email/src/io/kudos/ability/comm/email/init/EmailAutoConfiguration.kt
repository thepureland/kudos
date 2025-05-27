package io.kudos.ability.comm.email.init

import io.kudos.base.logger.LoggerFactory
import io.kudos.context.init.IComponentInitializer
import org.soul.ability.comm.email.handler.EmailHandler
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import javax.annotation.PostConstruct


/**
 * 邮件发送自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configurable
open class EmailAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean
    open fun emailHandler() = EmailHandler()

    @PostConstruct
    override fun init() {
        LoggerFactory.getLogger(this).info("【kudos-ability-comm-email】初始化完成.")
    }

}