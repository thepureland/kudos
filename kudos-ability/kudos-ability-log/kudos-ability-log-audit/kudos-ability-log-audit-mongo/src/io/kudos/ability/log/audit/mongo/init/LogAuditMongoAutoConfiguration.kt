package io.kudos.ability.log.audit.mongo.init

import io.kudos.ability.log.audit.common.api.IAuditLogReadOnlyService
import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.mongo.repository.SysAuditLogRepository
import io.kudos.ability.log.audit.mongo.service.MongoAuditLogReadOnlyService
import io.kudos.ability.log.audit.mongo.service.MongoAuditService
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

/**
 * Wires the Mongo audit backend.
 *
 * Gating:
 *  - [ConditionalOnClass] [MongoTemplate]: avoid wiring when an upstream depender pulls the
 *    module transitively without intending to enable Mongo (e.g. a multi-backend bundle that
 *    only ever uses RDB).
 *  - Bean name `mongoAuditService` / `mongoAuditLogReadOnlyService` + `@ConditionalOnMissingBean(name = ...)`
 *    matches the wiring pattern used by `LogAuditRdbKtormAutoConfiguration` and
 *    `LogAuditRdbClickhouseAutoConfiguration`. This lets a single app coexist multiple audit
 *    backends (Mongo + ktorm during a migration, ClickHouse + Mongo for split read/write,
 *    etc.) — type-based gating would have prevented coexistence, forcing every admin call to
 *    qualify by name anyway. **NOT** marked `@Primary`: MQ wins by default for write side;
 *    qualified `@Resource` picks specific reads.
 *
 * [EnableMongoRepositories] is scoped to this module's [SysAuditLogRepository] package only — we
 * do not want to broaden the scan of the host app's @SpringBootApplication just by depending on
 * this jar.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(MongoTemplate::class)
@EnableMongoRepositories(basePackageClasses = [SysAuditLogRepository::class])
open class LogAuditMongoAutoConfiguration : IComponentInitializer {

    @Bean("mongoAuditService")
    @ConditionalOnMissingBean(name = ["mongoAuditService"])
    open fun mongoAuditService(repository: SysAuditLogRepository): IAuditService =
        MongoAuditService(repository)

    /**
     * Read-side companion. Bean-name guarded so the admin / forensics caller can
     * `@Resource("mongoAuditLogReadOnlyService")` even when multiple `IAuditLogReadOnlyService`
     * beans coexist (e.g. the ktorm impl is also on the classpath during a backend migration).
     */
    @Bean("mongoAuditLogReadOnlyService")
    @ConditionalOnMissingBean(name = ["mongoAuditLogReadOnlyService"])
    open fun mongoAuditLogReadOnlyService(
        mongoTemplate: org.springframework.data.mongodb.core.MongoTemplate,
    ): IAuditLogReadOnlyService = MongoAuditLogReadOnlyService(mongoTemplate)

    override fun getComponentName() = "kudos-ability-log-audit-mongo"
}
