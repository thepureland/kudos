package io.kudos.ability.log.audit.mq.init

import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.api.IMonitorService
import io.kudos.ability.log.audit.mq.beans.MqAuditService
import io.kudos.ability.log.audit.mq.beans.MqMonitorService
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource

/**
 * Auto-configuration class for log audit MQ.
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
     * MQ-delivery [IAuditService] implementation.
     *
     * `@Primary` makes this bean win when multiple `IAuditService` implementations exist (MQ + RDB both included) —
     * the business-side LogAuditAspect picks up this MQ version via `@Autowired(required=false)`.
     * To reverse priority (RDB first), the business side must override the bean itself.
     */
    @Bean
    @Primary
    open fun mqAuditService(): IAuditService = MqAuditService()

    /**
     * MQ-delivery [IMonitorService] implementation.
     *
     * `@Primary` ensures this bean wins over the SLF4J fallback (`LoggingMonitorService`) registered
     * by `kudos-ability-log-audit-common` whenever this MQ module is present on the classpath.
     * Apps that explicitly want the SLF4J fallback must exclude this module or override the bean.
     */
    @Bean
    @Primary
    open fun mqMonitorService(): IMonitorService = MqMonitorService()

    override fun getComponentName() = "kudos-ability-log-audit-mq"

}
