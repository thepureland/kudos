package io.kudos.ability.log.audit.mq.init

import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.mq.beans.MqAuditService
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource

/**
 * 日志审计-mq自动配置类
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
@PropertySource(
    value = ["classpath:kudos-ability-log-audit-mq.yml"],
    factory = YamlPropertySourceFactory::class
)
open class LogAuditMqAutoConfiguration : IComponentInitializer {

    /**
     * MQ 投递的 [IAuditService] 实现。
     *
     * `@Primary` 让本 bean 在多个 `IAuditService` 实现（MQ + RDB 同时引入）的情况下胜出——
     * 业务侧的 LogAuditAspect 通过 `@Autowired(required=false)` 拿到的就是这个 MQ 版本。
     * 想反过来（RDB 优先）的话需要业务方自己覆盖 bean。
     */
    @Bean
    @Primary
    open fun mqAuditService(): IAuditService = MqAuditService()

    override fun getComponentName() = "kudos-ability-log-audit-mq"

}
