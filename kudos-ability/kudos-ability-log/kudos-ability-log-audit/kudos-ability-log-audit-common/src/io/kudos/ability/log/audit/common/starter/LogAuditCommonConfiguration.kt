package io.kudos.ability.log.audit.common.starter

import io.kudos.ability.log.audit.common.annotation.LogAuditAspect
import io.kudos.ability.log.audit.common.annotation.WebLogAuditAspect
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * 审计日志通用切面装配。
 *
 * `LogAuditAspect` / `WebLogAuditAspect` 本身已经标 `@Component`，业务侧应用如果开了
 * 对应 package 的 `@ComponentScan` 也能直接拿到 bean——本类只是给业务侧另一条装配路径
 * （走 kudos 自有的 [IComponentInitializer] 调度器）。`@ConditionalOnMissingBean` 让
 * @Bean 方法在 @Component 已经注册的情况下让位，二选一不冲突。
 *
 * **@Configuration + open**：让 Spring CGLIB 代理本类——避免类内 bean 方法互调时
 * 旁路代理重新执行 bean 创建（虽然当前两个方法互不相调，但保留这个保护更稳）。
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
open class LogAuditCommonConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean
    open fun logAuditAspect() = LogAuditAspect()

    @Bean
    @ConditionalOnMissingBean
    open fun webLogAuditAspect() = WebLogAuditAspect()

    override fun getComponentName() = "kudos-ability-log-audit-common"
}
