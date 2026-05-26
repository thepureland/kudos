package io.kudos.ability.log.audit.rdb.ktorm.init

import io.kudos.ability.data.rdb.ktorm.init.KtormAutoConfiguration
import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.rdb.ktorm.service.RdbKtormAuditService
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Auto-configuration for RDB Ktorm persistence of audit logs.
 *
 * Wires [RdbKtormAuditService] as the [IAuditService] implementation. **Not** marked `@Primary` — when coexisting with
 * the MQ version in [io.kudos.ability.log.audit.mq.init.LogAuditMqAutoConfiguration], the MQ version wins by default
 * (`@Primary`) and the aspect gets MQ; deployments that want the reverse (RDB first) can override on the business side
 * with `@Bean(name="auditService") @Primary` pointing at an instance of this class.
 *
 * **Switch**: `kudos.ability.log.audit.rdb.ktorm.enabled=false` disables this module's bean wiring; `matchIfMissing
 * = true` makes the default behavior "enabled".
 *
 * **DDL**: the create-table script is shipped with the [io.kudos.ability.log.audit.rdb.common] module at
 * `classpath:db/migration/V20260519__create_sys_audit_log.sql`. With `kudos-ability-data-rdb-flyway` it runs
 * automatically; deployments not using flyway must run it manually.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(KtormAutoConfiguration::class)
@ConditionalOnProperty(
    prefix = "kudos.ability.log.audit.rdb.ktorm",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
open class LogAuditRdbKtormAutoConfiguration : IComponentInitializer {

    @Bean("rdbKtormAuditService")
    @ConditionalOnMissingBean(name = ["rdbKtormAuditService"])
    open fun rdbKtormAuditService(): IAuditService = RdbKtormAuditService()

    override fun getComponentName() = "kudos-ability-log-audit-rdb-ktorm"
}
