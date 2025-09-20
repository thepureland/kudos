package io.kudos.ability.log.audit.commobn.starter

import io.kudos.ability.log.audit.commobn.annotation.LogAuditAspect
import io.kudos.ability.log.audit.commobn.annotation.WebLogAuditAspect
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
