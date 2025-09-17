package io.kudos.ability.log.audit.mq.init

import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.context.config.YamlPropertySourceFactory
import org.soul.ability.log.audit.common.api.IAuditService
import org.soul.ability.log.audit.mq.beans.MqAuditService
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource

/**
 * 日志审计-mq自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
@PropertySource(
    value = ["classpath:kudos-ability-log-audit-mq.yml"],
    factory = YamlPropertySourceFactory::class
)
open class LogAuditMqAutoConfiguration : IComponentInitializer {

    @Bean
    @Primary // LogAudit如果同时引入mq，则优先使用mq发送日志
    open fun mqAuditService(): IAuditService = MqAuditService()

    override fun getComponentName() = "kudos-ability-log-audit-mq"

}
