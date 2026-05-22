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
 * 审计日志 RDB Ktorm 落地的自动配置。
 *
 * 装配 [RdbKtormAuditService] 作为 [IAuditService] 实现。**不**用 `@Primary`——和
 * [io.kudos.ability.log.audit.mq.init.LogAuditMqAutoConfiguration] 的 MQ 版本同时存在时，
 * MQ 版本默认胜出（`@Primary`），切面拿到的是 MQ；想反过来（RDB 优先）的部署可以
 * 在业务侧自己覆盖 `@Bean(name="auditService") @Primary` 指向本类的实例。
 *
 * **开关**：`kudos.ability.log.audit.rdb.ktorm.enabled=false` 可关掉本模块的 bean 装配；
 * `matchIfMissing = true` 让默认行为是开。
 *
 * **DDL**：建表脚本随 [io.kudos.ability.log.audit.rdb.common] 模块发布在
 * `classpath:db/migration/V20260519__create_sys_audit_log.sql`。配合
 * `kudos-ability-data-rdb-flyway` 自动执行；不走 flyway 的部署需要手动执行。
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
