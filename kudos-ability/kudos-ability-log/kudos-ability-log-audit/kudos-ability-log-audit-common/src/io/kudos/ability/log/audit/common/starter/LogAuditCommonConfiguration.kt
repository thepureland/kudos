package io.kudos.ability.log.audit.common.starter

import io.kudos.ability.log.audit.common.annotation.LogAuditAspect
import io.kudos.ability.log.audit.common.annotation.WebLogAuditAspect
import io.kudos.ability.log.audit.common.api.IMonitorService
import io.kudos.ability.log.audit.common.api.LoggingMonitorService
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * Common aspect wiring for the audit log.
 *
 * `LogAuditAspect` / `WebLogAuditAspect` themselves are already `@Component`-annotated; a business application that
 * enables `@ComponentScan` on the corresponding package can also pick up the beans directly — this class merely
 * provides an alternative wiring path (via kudos's own [IComponentInitializer] dispatcher).
 * `@ConditionalOnMissingBean` makes the `@Bean` methods step aside when the `@Component` version is already
 * registered, so either route works without conflict.
 *
 * **@Configuration + open**: allows Spring CGLIB to proxy this class — guarding against in-class bean-method
 * self-invocation bypassing the proxy and re-running bean creation (although the two methods here do not call each
 * other today, keeping this protection is safer).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
open class LogAuditCommonConfiguration : IComponentInitializer {

    /**
     * General method-level audit aspect.
     *
     * @return a [LogAuditAspect] instance
     * @author K
     * @since 1.0.0
     */
    @Bean
    @ConditionalOnMissingBean
    open fun logAuditAspect() = LogAuditAspect()

    /**
     * Web-call audit aspect.
     *
     * @return a [WebLogAuditAspect] instance
     * @author K
     * @since 1.0.0
     */
    @Bean
    @ConditionalOnMissingBean
    open fun webLogAuditAspect() = WebLogAuditAspect()

    /**
     * Default [IMonitorService] fallback that logs every submitted
     * [io.kudos.ability.log.audit.common.entity.SysMonitorMsgVo] at ERROR level via slf4j.
     * Activated only when no other `IMonitorService` bean exists — apps pulling in
     * `kudos-ability-log-audit-mq` (or wiring a custom one) automatically replace this.
     *
     * Closes the long-standing "IMonitorService interface with no default implementation"
     * gap: before this, any call to
     * [io.kudos.ability.log.audit.common.support.MonitorMsgTool.pushErrMsg] on an app without an
     * MQ module would explode with `NoSuchBeanDefinitionException` during an existing error-
     * handling flow.
     */
    @Bean
    @ConditionalOnMissingBean(IMonitorService::class)
    open fun loggingMonitorService(): IMonitorService = LoggingMonitorService()

    override fun getComponentName() = "kudos-ability-log-audit-common"
}
