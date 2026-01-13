package io.kudos.ability.log.audit.common.starter

import io.kudos.ability.log.audit.common.annotation.LogAuditAspect
import io.kudos.ability.log.audit.common.annotation.WebLogAuditAspect
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean


class LogAuditCommonConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun logAuditAspect()= LogAuditAspect()

    @Bean
    @ConditionalOnMissingBean
    fun webLogAuditAspect()= WebLogAuditAspect()


}
