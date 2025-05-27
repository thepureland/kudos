package io.kudos.ability.log.audit.mq.init

import io.kudos.context.spring.YamlPropertySourceFactory
import jakarta.annotation.PostConstruct
import org.soul.ability.log.audit.common.api.IAuditService
import org.soul.ability.log.audit.mq.beans.MqAuditService
import org.soul.ability.log.audit.mq.starter.LogAuditMqConfiguration
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource

@Configuration
@PropertySource(value = ["classpath:kudos-ability-log-audit-mq.yml"], factory = YamlPropertySourceFactory::class)
class LogAuditMqAutoConfiguration {
    /**
     * LogAudit如果同时引入mq，则优先使用mq发送日志
     */
    @Bean
    @Primary
    fun mqAuditService(): IAuditService {
        return MqAuditService()
    }

    @PostConstruct
    fun init() {
        LOG.info("[soul-ability-log-audit-mq]初始化完成...")
    }

    companion object {
        private val LOG: Log = LogFactory.getLog(LogAuditMqConfiguration::class.java)
    }
}
