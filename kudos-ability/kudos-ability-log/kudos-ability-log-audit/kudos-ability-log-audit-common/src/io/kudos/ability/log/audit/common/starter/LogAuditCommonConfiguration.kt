package io.kudos.ability.log.audit.common.starter

import io.kudos.ability.log.audit.common.annotation.LogAuditAspect
import io.kudos.ability.log.audit.common.annotation.WebLogAuditAspect
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean


/**
 * 审计日志公共自动配置。
 *
 * 注册两个切面 bean：[LogAuditAspect]（普通方法级审计）与 [WebLogAuditAspect]（Web 调用审计）。
 * 都加 `@ConditionalOnMissingBean` 让上层应用可自定义实现覆盖默认行为。
 *
 * @author K
 * @since 1.0.0
 */
class LogAuditCommonConfiguration {

    /**
     * 普通方法级审计切面。
     *
     * @return [LogAuditAspect] 实例
     * @author K
     * @since 1.0.0
     */
    @Bean
    @ConditionalOnMissingBean
    fun logAuditAspect()= LogAuditAspect()

    /**
     * Web 调用审计切面。
     *
     * @return [WebLogAuditAspect] 实例
     * @author K
     * @since 1.0.0
     */
    @Bean
    @ConditionalOnMissingBean
    fun webLogAuditAspect()= WebLogAuditAspect()


}
