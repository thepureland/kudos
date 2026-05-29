package io.kudos.ability.log.audit.rdb.clickhouse.init

import io.kudos.ability.data.rdb.ktorm.init.KtormAutoConfiguration
import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.rdb.clickhouse.service.RdbClickhouseAuditService
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Wires the ClickHouse audit backend.
 *
 * Mirrors `LogAuditRdbKtormAutoConfiguration`'s style:
 *  - `@AutoConfigureAfter(KtormAutoConfiguration)` so the ktorm `Database` is already wired.
 *  - `@ConditionalOnProperty` switch `kudos.ability.log.audit.rdb.clickhouse.enabled` (default
 *    true) lets apps depend on the module without forcing it on in every profile.
 *  - Bean named `clickhouseAuditService` + `@ConditionalOnMissingBean(name = ...)`. Apps already
 *    declaring their own bean of the same name win — same pattern the ktorm module uses, so
 *    sibling modules don't accidentally shadow each other.
 *
 * Crucially **NOT** marked `@Primary`. When this module coexists with the ktorm or MQ backend,
 * the resolver-side either picks via `@Primary` (MQ does this by default) or by `@Qualifier`.
 * Single-backend deployments — the common case — work without extra wiring because there's only
 * one `IAuditService` candidate.
 *
 * The service resolves the active ktorm `Database` per-call (via `KudosContextHolder.currentDatabase()`)
 * — not at bean creation — so the kudos dynamic-datasource layer can route per-tenant.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(KtormAutoConfiguration::class)
@ConditionalOnProperty(
    prefix = "kudos.ability.log.audit.rdb.clickhouse",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
open class LogAuditRdbClickhouseAutoConfiguration : IComponentInitializer {

    @Bean("clickhouseAuditService")
    @ConditionalOnMissingBean(name = ["clickhouseAuditService"])
    open fun clickhouseAuditService(): IAuditService = RdbClickhouseAuditService()

    override fun getComponentName() = "kudos-ability-log-audit-rdb-clickhouse"
}
